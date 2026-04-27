package ml.melun.mangaview.mangaview;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WfwfDomainResolver {
    private static final Pattern WFWF_PATTERN = Pattern.compile("^https?://wfwf(\\d+)\\.com(?:/cm)?/?$");
    private static final int DEFAULT_NUMBER = 449;
    private static final int FORWARD_SCAN_LIMIT = 300;
    private static final int BACKWARD_SCAN_LIMIT = 5;
    private static final int PARALLEL_PROBES = 10;

    public static String resolve(OkHttpClient client, String currentUrl, Map<String, String> headers) {
        int current = getNumber(currentUrl);
        if(current <= 0)
            current = DEFAULT_NUMBER;

        OkHttpClient probeClient = client.newBuilder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .callTimeout(5, TimeUnit.SECONDS)
                .build();

        String currentRoot = "https://wfwf" + current + ".com";
        if(isAlive(probeClient, currentRoot, headers))
            return currentRoot;

        return findAliveCandidate(probeClient, candidates(current), headers);
    }

    public static boolean isWfwfUrl(String url) {
        return getNumber(url) > 0;
    }

    public static String toRoot(String url) {
        if(url == null)
            return "";
        String trimmed = trimTrailingSlash(url);
        if(trimmed.endsWith("/cm"))
            return trimmed.substring(0, trimmed.length() - 3);
        return trimmed;
    }

    private static int getNumber(String url) {
        if(url == null)
            return -1;
        Matcher matcher = WFWF_PATTERN.matcher(trimTrailingSlash(url));
        if(!matcher.matches())
            return -1;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (Exception e) {
            return -1;
        }
    }

    private static List<Integer> candidates(int current) {
        ArrayList<Integer> numbers = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for(int i = 1; i <= FORWARD_SCAN_LIMIT; i++)
            add(numbers, seen, current + i);
        add(numbers, seen, DEFAULT_NUMBER);
        for(int i = 1; i <= FORWARD_SCAN_LIMIT; i++)
            add(numbers, seen, DEFAULT_NUMBER + i);
        for(int i = 1; i <= BACKWARD_SCAN_LIMIT; i++)
            add(numbers, seen, current - i);
        return numbers;
    }

    private static String findAliveCandidate(OkHttpClient client, List<Integer> candidates, Map<String, String> headers) {
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_PROBES);
        try {
            for(int start = 0; start < candidates.size(); start += PARALLEL_PROBES) {
                int end = Math.min(start + PARALLEL_PROBES, candidates.size());
                ArrayList<Future<Boolean>> futures = new ArrayList<>();
                ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<>(executor);
                for(int i = start; i < end; i++) {
                    final int number = candidates.get(i);
                    futures.add(completionService.submit(new Callable<Boolean>() {
                        @Override
                        public Boolean call() {
                            return isAlive(client, "https://wfwf" + number + ".com", headers);
                        }
                    }));
                }

                for(int i = start; i < end; i++)
                    completionService.take();

                for(int i = 0; i < futures.size(); i++) {
                    try {
                        if(futures.get(i).get())
                            return "https://wfwf" + candidates.get(start + i) + ".com";
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            executor.shutdownNow();
        }
        return null;
    }

    private static void add(List<Integer> numbers, Set<Integer> seen, int number) {
        if(number > 0 && seen.add(number))
            numbers.add(number);
    }

    private static boolean isAlive(OkHttpClient client, String root, Map<String, String> headers) {
        return probe(client, root + "/ing", headers) || probe(client, root + "/cm", headers);
    }

    private static boolean probe(OkHttpClient client, String url, Map<String, String> headers) {
        Response response = null;
        try {
            Request.Builder builder = new Request.Builder().url(url).get();
            if(headers != null)
                for(String key : headers.keySet())
                    builder.addHeader(key, headers.get(key));
            response = client.newCall(builder.build()).execute();
            int code = response.code();
            String body = response.body() == null ? "" : response.body().string();
            return code >= 200 && code < 500 && looksLikeWfwf(body);
        } catch (Exception e) {
            return false;
        } finally {
            if(response != null)
                response.close();
        }
    }

    private static boolean looksLikeWfwf(String body) {
        if(body == null)
            return false;
        String lower = body.toLowerCase();
        return lower.contains("webtoon-list")
                || lower.contains("toon=")
                || lower.contains("/view?toon=")
                || lower.contains("/list?toon=")
                || lower.contains("/cv?toon=")
                || lower.contains("/cl?toon=");
    }

    private static String trimTrailingSlash(String url){
        String trimmed = url.trim();
        while(trimmed.endsWith("/"))
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        return trimmed;
    }
}

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.*;
import javax.imageio.ImageIO;

/**
 * Social Media Reach Analysis - Task 2 (Java version, no external libraries)
 *
 * Steps: 1) Create dataset  2) Clean  3) Feature engineering
 *        4) Analysis        5) Charts (PNG) 6) Insights
 *
 * Note: Dataset is simulated with realistic patterns. To use real data,
 * replace generateData() with a CSV loader that fills the same Post fields.
 */
public class SocialMediaReachAnalysis {
    // ---------- Data model ----------
    static class Post {
        int id;
        LocalDate date;
        String platform, contentType;
        int hour, followers;
        Integer hashtags, likes, comments;   // Integer so they can be null (missing)
        int shares;
        long reach, impressions;
        long engagement;
        double engagementRate;

        String key() {
            return date + "|" + platform + "|" + contentType + "|" + hour + "|" + followers + "|" + reach;
        }
    }

    static final String[] PLATFORMS = {"Instagram", "Facebook", "Twitter/X", "LinkedIn", "YouTube"};
    static final double[] PLATFORM_P = {0.28, 0.22, 0.18, 0.14, 0.18};
    static final double[] PLATFORM_M = {1.0, 0.8, 0.65, 0.55, 1.2};

    static final String[] TYPES = {"Image", "Video", "Reel/Short", "Text", "Carousel"};
    static final double[] TYPE_P = {0.22, 0.20, 0.25, 0.13, 0.20};
    static final double[] TYPE_M = {0.9, 1.2, 1.8, 0.6, 1.1};

    static final int[] HOUR_W = {1,1,1,1,1,2,3,5,6,6,5,5,6,6,5,5,6,7,9,10,9,7,4,2};

    static final Random RNG = new Random(42);
    static String OUT = "";

    // ---------- 1. Dataset creation ----------
    static int pick(double[] probs) {
        double r = RNG.nextDouble(), cum = 0;
        for (int i = 0; i < probs.length; i++) { cum += probs[i]; if (r < cum) return i; }
        return probs.length - 1;
    }

    static int pickHour() {
        int total = Arrays.stream(HOUR_W).sum();
        int r = RNG.nextInt(total), cum = 0;
        for (int h = 0; h < 24; h++) { cum += HOUR_W[h]; if (r < cum) return h; }
        return 23;
    }

    static List<Post> generateData(int n) {
        List<Post> list = new ArrayList<>();
        LocalDate start = LocalDate.of(2025, 1, 1);
        for (int i = 1; i <= n; i++) {
            Post p = new Post();
            p.id = i;
            p.date = start.plusDays(RNG.nextInt(365));
            int pi = pick(PLATFORM_P), ti = pick(TYPE_P);
            p.platform = PLATFORMS[pi];
            p.contentType = TYPES[ti];
            p.hour = pickHour();
            p.followers = 5000 + RNG.nextInt(115000);
            p.hashtags = RNG.nextInt(16);

            double base = p.followers * 0.25;
            double hourBoost = (p.hour >= 18 && p.hour <= 21) ? 1.35 : 1.0;
            double tagBoost = 1 + 0.03 * Math.min(p.hashtags, 8) - 0.02 * Math.max(p.hashtags - 10, 0);
            double noise = Math.exp(RNG.nextGaussian() * 0.35);

            p.reach = (long) (base * PLATFORM_M[pi] * TYPE_M[ti] * hourBoost * tagBoost * noise);
            p.impressions = (long) (p.reach * (1.1 + RNG.nextDouble() * 0.7));
            p.likes = (int) (p.reach * (0.02 + RNG.nextDouble() * 0.06));
            p.comments = (int) (p.likes * (0.05 + RNG.nextDouble() * 0.15));
            p.shares = (int) (p.likes * (0.03 + RNG.nextDouble() * 0.12));
            list.add(p);
        }
        // inject ~2% missing values to demonstrate cleaning
        for (Post p : list) {
            if (RNG.nextDouble() < 0.02) p.likes = null;
            if (RNG.nextDouble() < 0.02) p.comments = null;
            if (RNG.nextDouble() < 0.02) p.hashtags = null;
        }
        // inject 10 duplicate rows
        for (int i = 0; i < 10; i++) list.add(list.get(RNG.nextInt(n)));
        return list;
    }

    static void saveCsv(List<Post> posts, String file) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(file))) {
            w.println("post_id,date,platform,content_type,post_hour,followers,hashtags,reach,impressions,likes,comments,shares");
            for (Post p : posts) {
                w.printf("%d,%s,%s,%s,%d,%d,%s,%d,%d,%s,%s,%d%n", p.id, p.date, p.platform, p.contentType,
                        p.hour, p.followers, p.hashtags == null ? "" : p.hashtags, p.reach, p.impressions,
                        p.likes == null ? "" : p.likes, p.comments == null ? "" : p.comments, p.shares);
            }
        }
    }

    // ---------- 2. Cleaning ----------
    static double median(List<Integer> vals) {
        List<Integer> s = vals.stream().sorted().collect(Collectors.toList());
        return s.isEmpty() ? 0 : s.get(s.size() / 2);
    }

    static List<Post> clean(List<Post> raw) {
        System.out.println("Raw rows: " + raw.size());
        long missing = raw.stream().filter(p -> p.likes == null || p.comments == null || p.hashtags == null).count();
        System.out.println("Rows with missing values: " + missing);

        // remove duplicates (keep first occurrence)
        Set<Integer> seenIds = new HashSet<>();
        List<Post> unique = new ArrayList<>();
        for (Post p : raw) if (seenIds.add(p.id)) unique.add(p);
        System.out.println("Duplicates removed: " + (raw.size() - unique.size()));

        // fill missing with median
        int medLikes = (int) median(unique.stream().filter(p -> p.likes != null).map(p -> p.likes).collect(Collectors.toList()));
        int medComm = (int) median(unique.stream().filter(p -> p.comments != null).map(p -> p.comments).collect(Collectors.toList()));
        int medTags = (int) median(unique.stream().filter(p -> p.hashtags != null).map(p -> p.hashtags).collect(Collectors.toList()));
        for (Post p : unique) {
            if (p.likes == null) p.likes = medLikes;
            if (p.comments == null) p.comments = medComm;
            if (p.hashtags == null) p.hashtags = medTags;
        }
        System.out.println("Clean rows: " + unique.size());
        return unique;
    }

    // ---------- 3. Feature engineering ----------
    static void addFeatures(List<Post> posts) {
        for (Post p : posts) {
            p.engagement = (long) p.likes + p.comments + p.shares;
            p.engagementRate = p.reach == 0 ? 0 : p.engagement * 100.0 / p.reach;
        }
    }

    // ---------- 4. Analysis helpers ----------
    static <K extends Comparable<K>> Map<K, Double> avgBy(List<Post> posts,
            java.util.function.Function<Post, K> keyFn, java.util.function.ToDoubleFunction<Post> valFn) {
        return posts.stream().collect(Collectors.groupingBy(keyFn, TreeMap::new, Collectors.averagingDouble(valFn)));
    }

    static <K> Map<K, Double> sortDesc(Map<K, Double> m) {
        return m.entrySet().stream().sorted(Map.Entry.<K, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    static double correlation(double[] x, double[] y) {
        int n = x.length;
        double mx = Arrays.stream(x).average().orElse(0), my = Arrays.stream(y).average().orElse(0);
        double sxy = 0, sxx = 0, syy = 0;
        for (int i = 0; i < n; i++) {
            sxy += (x[i] - mx) * (y[i] - my);
            sxx += (x[i] - mx) * (x[i] - mx);
            syy += (y[i] - my) * (y[i] - my);
        }
        return sxy / Math.sqrt(sxx * syy);
    }

    static void printTable(String title, Map<String, Double> reach, Map<String, Double> eng) {
        System.out.println("\n" + title);
        System.out.printf("%-14s %12s %14s%n", "Category", "Avg Reach", "Avg Eng Rate%");
        for (String k : reach.keySet())
            System.out.printf("%-14s %12.0f %14.2f%n", k, reach.get(k), eng.get(k));
    }

    static void saveSummary(String file, Map<String, Double> reach, Map<String, Double> eng) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(file))) {
            w.println("category,avg_reach,avg_engagement_rate");
            for (String k : reach.keySet()) w.printf("%s,%.2f,%.2f%n", k, reach.get(k), eng.get(k));
        }
    }

    // ---------- 5. Charts (pure Java2D, no libraries) ----------
    static final Color PRIMARY = new Color(79, 70, 229);
    static final Color LIGHT = new Color(199, 210, 254);

    static void barChart(String title, String yLabel, Map<String, Double> data, String file,
                         Set<String> highlight) throws IOException {
        int W = 900, H = 520, L = 90, R = 30, T = 70, B = 80;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, W, H);

        g.setColor(new Color(17, 24, 39)); g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString(title, L, 40);

        double max = data.values().stream().mapToDouble(d -> d).max().orElse(1) * 1.1;
        int plotW = W - L - R, plotH = H - T - B;

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i <= 5; i++) {                       // grid + y labels
            int y = T + plotH - (int) (plotH * i / 5.0);
            g.setColor(new Color(229, 231, 235)); g.drawLine(L, y, W - R, y);
            g.setColor(Color.DARK_GRAY);
            g.drawString(String.format("%,.0f", max * i / 5), 10, y + 4);
        }
        g.rotate(-Math.PI / 2); g.drawString(yLabel, -T - plotH / 2 - 30, 18); g.rotate(Math.PI / 2);

        int n = data.size(), i = 0;
        double slot = (double) plotW / n;
        for (Map.Entry<String, Double> e : data.entrySet()) {
            int bw = (int) (slot * 0.65);
            int x = L + (int) (i * slot + (slot - bw) / 2);
            int bh = (int) (plotH * e.getValue() / max);
            g.setColor(highlight == null || highlight.contains(e.getKey()) ? PRIMARY : LIGHT);
            g.fillRoundRect(x, T + plotH - bh, bw, bh, 6, 6);
            g.setColor(Color.DARK_GRAY);
            String lbl = e.getKey();
            g.drawString(lbl, x + bw / 2 - g.getFontMetrics().stringWidth(lbl) / 2, T + plotH + 20);
            String v = String.format("%,.0f", e.getValue());
            if (n <= 8) g.drawString(v, x + bw / 2 - g.getFontMetrics().stringWidth(v) / 2, T + plotH - bh - 6);
            i++;
        }
        g.dispose();
        ImageIO.write(img, "png", new File(file));
    }

    static void lineChart(String title, String yLabel, Map<String, Double> data, String file) throws IOException {
        int W = 900, H = 520, L = 90, R = 30, T = 70, B = 80;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, W, H);
        g.setColor(new Color(17, 24, 39)); g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString(title, L, 40);

        double max = data.values().stream().mapToDouble(d -> d).max().orElse(1) * 1.1;
        double min = 0;
        int plotW = W - L - R, plotH = H - T - B;
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i <= 5; i++) {
            int y = T + plotH - (int) (plotH * i / 5.0);
            g.setColor(new Color(229, 231, 235)); g.drawLine(L, y, W - R, y);
            g.setColor(Color.DARK_GRAY);
            g.drawString(String.format("%,.0f", (max - min) * i / 5 + min), 10, y + 4);
        }
        g.rotate(-Math.PI / 2); g.drawString(yLabel, -T - plotH / 2 - 30, 18); g.rotate(Math.PI / 2);

        int n = data.size(), i = 0, px = -1, py = -1;
        g.setStroke(new BasicStroke(3f));
        for (Map.Entry<String, Double> e : data.entrySet()) {
            int x = L + (int) (plotW * (i + 0.5) / n);
            int y = T + plotH - (int) (plotH * (e.getValue() - min) / (max - min));
            g.setColor(PRIMARY);
            if (px >= 0) g.drawLine(px, py, x, y);
            g.fillOval(x - 5, y - 5, 10, 10);
            g.setColor(Color.DARK_GRAY);
            g.drawString(e.getKey(), x - g.getFontMetrics().stringWidth(e.getKey()) / 2, T + plotH + 20);
            px = x; py = y; i++;
        }
        g.dispose();
        ImageIO.write(img, "png", new File(file));
    }

    // ---------- Main ----------
    public static void main(String[] args) throws IOException {
        // CSV and charts are saved in the folder you run the program from
        OUT = "";

        // 1. dataset
        List<Post> raw = generateData(1000);
        saveCsv(raw, OUT + "social_media_data.csv");

        // 2 & 3. clean + features
        List<Post> posts = clean(raw);
        addFeatures(posts);

        // 4. analysis
        Map<String, Double> platReach = sortDesc(SocialMediaReachAnalysis.<String>avgBy(posts, p -> p.platform, p -> p.reach));
        Map<String, Double> platEng = avgBy(posts, p -> p.platform, p -> p.engagementRate);
        Map<String, Double> typeReach = sortDesc(SocialMediaReachAnalysis.<String>avgBy(posts, p -> p.contentType, p -> p.reach));
        Map<String, Double> typeEng = avgBy(posts, p -> p.contentType, p -> p.engagementRate);

        printTable("=== Reach by Platform ===", platReach, platEng);
        printTable("=== Reach by Content Type ===", typeReach, typeEng);
        saveSummary(OUT + "summary_by_platform.csv", platReach, platEng);
        saveSummary(OUT + "summary_by_content_type.csv", typeReach, typeEng);

        Map<Integer, Double> hourReachI = avgBy(posts, p -> p.hour, p -> p.reach);
        Map<String, Double> hourReach = new LinkedHashMap<>();
        hourReachI.forEach((h, v) -> hourReach.put(String.valueOf(h), v));

        Map<Integer, Double> monthI = avgBy(posts, p -> p.date.getMonthValue(), p -> p.reach);
        Map<String, Double> monthReach = new LinkedHashMap<>();
        monthI.forEach((m, v) -> monthReach.put(java.time.Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH), v));

        Map<String, Double> weekdayReach = new LinkedHashMap<>();
        Map<java.time.DayOfWeek, Double> wk = avgBy(posts, p -> p.date.getDayOfWeek(), p -> p.reach);
        for (java.time.DayOfWeek d : java.time.DayOfWeek.values())
            weekdayReach.put(d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), wk.get(d));

        double peak = posts.stream().filter(p -> p.hour >= 18 && p.hour <= 21).mapToLong(p -> p.reach).average().orElse(0);
        double off = posts.stream().filter(p -> p.hour < 18 || p.hour > 21).mapToLong(p -> p.reach).average().orElse(1);
        double corr = correlation(posts.stream().mapToDouble(p -> p.followers).toArray(),
                                  posts.stream().mapToDouble(p -> p.reach).toArray());
        double reelVsText = typeReach.get("Reel/Short") / typeReach.get("Text");
        String bestDay = weekdayReach.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();

        // 5. charts
        barChart("Average Reach by Platform", "Avg Reach", platReach, OUT + "1_reach_by_platform.png", null);
        barChart("Average Reach by Content Type", "Avg Reach", typeReach, OUT + "2_reach_by_content_type.png", null);
        lineChart("Monthly Average Reach Trend", "Avg Reach", monthReach, OUT + "3_monthly_trend.png");
        Set<String> peakHours = new HashSet<>(Arrays.asList("18", "19", "20", "21"));
        barChart("Average Reach by Posting Hour (peak highlighted)", "Avg Reach", hourReach,
                OUT + "4_reach_by_hour.png", peakHours);
        barChart("Average Reach by Weekday", "Avg Reach", weekdayReach, OUT + "5_reach_by_weekday.png", null);

        // 6. insights
        System.out.println("\n=== KEY INSIGHTS ===");
        System.out.println("Top platform by reach     : " + platReach.keySet().iterator().next());
        System.out.println("Top content type by reach : " + typeReach.keySet().iterator().next());
        System.out.printf("Reel/Short vs Text reach  : %.1fx%n", reelVsText);
        System.out.printf("Peak hours (18-21) uplift : %.0f%%%n", (peak / off - 1) * 100);
        System.out.printf("Followers vs reach corr   : %.2f%n", corr);
        System.out.println("Best weekday              : " + bestDay);
        System.out.println("\nDone. CSV files and charts saved in: " + new File(".").getAbsolutePath());
    }
}
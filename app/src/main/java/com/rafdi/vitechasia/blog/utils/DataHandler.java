package com.rafdi.vitechasia.blog.utils;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.rafdi.vitechasia.blog.models.Article;
import com.rafdi.vitechasia.blog.models.Category;
import com.rafdi.vitechasia.blog.viewmodel.ArticleViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Utility class for managing article data.
 * Tries to fetch data from the API first, falls back to dummy data if needed.
 */
public class DataHandler {
    private static volatile DataHandler instance;
    private ArticleViewModel articleViewModel;
    private boolean isViewModelInitialized = false;

    /**
     * Interface for receiving data load callbacks
     */
    public interface DataLoadListener {
        void onDataLoaded(List<Article> articles);

        void onError(String message);
    }

    /**
     * Interface for receiving single article callbacks
     */
    public interface SingleArticleCallback {
        void onArticleLoaded(Article article);

        void onError(String message);
    }

    private DataHandler() {
        // Private constructor to prevent direct instantiation
    }

    /**
     * Initializes the DataHandler with a Context.
     * Must be called before using any other methods.
     *
     * @param context The application context
     */
    public static void initialize(Context context) {
        if (instance == null) {
            synchronized (DataHandler.class) {
                if (instance == null) {
                    instance = new DataHandler();
                    if (context != null && context.getApplicationContext() instanceof Application) {
                        instance.articleViewModel = new ArticleViewModel((Application) context.getApplicationContext());
                        instance.isViewModelInitialized = true;
                    }
                }
            }
        }
    }

    /**
     * Gets the singleton instance of DataHandler.
     * initialize() must be called first.
     *
     * @return The DataHandler instance
     * @throws IllegalStateException if initialize() hasn't been called
     */
    public static DataHandler getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DataHandler must be initialized first. Call initialize() in your Application class.");
        }
        return instance;
    }

    private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
            "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
            "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris.";

    // Category IDs
    public static final String CATEGORY_TECH = "tech";
    public static final String CATEGORY_HEALTH = "health";
    public static final String CATEGORY_LIFESTYLE = "lifestyle";
    public static final String CATEGORY_BUSINESS = "business";
    public static final String CATEGORY_SPORTS = "sports";
    public static final String CATEGORY_NEWS = "news";

    // Subcategory IDs
    public static final String SUBCATEGORY_ANDROID = "android";
    public static final String SUBCATEGORY_IOS = "ios";
    public static final String SUBCATEGORY_WEB = "web";
    public static final String SUBCATEGORY_AI = "ai";
    public static final String SUBCATEGORY_SUSTAINABILITY = "sustainability";
    public static final String SUBCATEGORY_FINANCE = "finance";
    public static final String SUBCATEGORY_FITNESS = "fitness";
    public static final String SUBCATEGORY_NUTRITION = "nutrition";
    public static final String SUBCATEGORY_MENTAL_HEALTH = "mental health";
    public static final String SUBCATEGORY_TRAVEL = "travel";
    public static final String SUBCATEGORY_FOOD = "food";
    public static final String SUBCATEGORY_FASHION = "fashion";
    public static final String SUBCATEGORY_FOOTBALL = "football";
    public static final String SUBCATEGORY_BASKETBALL = "basketball";
    public static final String SUBCATEGORY_TENNIS = "tennis";
    public static final String SUBCATEGORY_WORLD = "world";
    public static final String SUBCATEGORY_POLITICS = "politics";
    public static final String SUBCATEGORY_ECONOMY = "economy";

    private static List<Article> allArticles = null;
    private boolean isLoading = false;
    private DataLoadListener dataLoadListener;

    /**
     * Get all bookmarked articles, synced with BookmarkManager
     */
    public static List<Article> getBookmarkedArticles(android.content.Context context) {
        List<Article> allArticles = getDummyArticles();
        BookmarkManager bookmarkManager = BookmarkManager.getInstance(context);
        bookmarkManager.syncArticleBookmarkStatus(allArticles);

        List<Article> bookmarked = new ArrayList<>();
        for (Article article : allArticles) {
            if (article.isBookmarked()) {
                bookmarked.add(article);
            }
        }
        return bookmarked;
    }

    /**
     * Get all dummy articles. The list is cached after first creation.
     */
    public static List<Article> getDummyArticles() {
        if (allArticles == null) {
            allArticles = generateDummyArticles();
        }
        return new ArrayList<>(allArticles); // Return a copy to prevent modification of cached list
    }

    /**
     * Get articles filtered by subcategory ID, trying the API first and falling back to dummy data
     *
     * @param subcategoryId The subcategory ID to filter by
     * @param callback     Callback to receive the results asynchronously
     */
    public void getArticlesBySubcategory(String subcategoryId, DataLoadListener callback) {
        if (!isViewModelInitialized) {
            // Fall back to dummy data if ViewModel isn't initialized
            List<Article> dummyArticles = getDummyArticlesBySubcategory(subcategoryId);
            if (callback != null) {
                callback.onDataLoaded(dummyArticles);
            }
            return;
        }

        // Try to get data from the API first using the subcategory as a category filter
        articleViewModel.loadArticles(subcategoryId, 1, 20); // Default page 1, 20 items per page
        articleViewModel.getArticlesLiveData().observeForever(new Observer<List<Article>>() {
            @Override
            public void onChanged(List<Article> articles) {
                if (articles != null && !articles.isEmpty()) {
                    if (callback != null) {
                        callback.onDataLoaded(articles);
                    }
                    articleViewModel.getArticlesLiveData().removeObserver(this);
                } else {
                    // Fall back to dummy data if API returns empty
                    List<Article> dummyArticles = getDummyArticlesBySubcategory(subcategoryId);
                    if (callback != null) {
                        callback.onDataLoaded(dummyArticles);
                    }
                    articleViewModel.getArticlesLiveData().removeObserver(this);
                }
            }
        });

        // Set up error handling
        articleViewModel.getErrorLiveData().observeForever(new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null) {
                    // On error, fall back to dummy data
                    List<Article> dummyArticles = getDummyArticlesBySubcategory(subcategoryId);
                    if (callback != null) {
                        callback.onDataLoaded(dummyArticles);
                    }
                    articleViewModel.getErrorLiveData().removeObserver(this);
                }
            }
        });
    }

    /**
     * Get dummy articles filtered by subcategory ID
     *
     * @param subcategoryId The subcategory ID to filter by
     * @return List of articles in the specified subcategory, or empty list if none found
     */
    public static List<Article> getDummyArticlesBySubcategory(String subcategoryId) {
        if (subcategoryId == null) {
            return new ArrayList<>();
        }
        
        List<Article> allArticles = getDummyArticles();
        List<Article> filteredArticles = new ArrayList<>();
        
        for (Article article : allArticles) {
            if (subcategoryId.equalsIgnoreCase(article.getSubcategoryId())) {
                filteredArticles.add(article);
            }
        }
        
        return filteredArticles;
    }
    
    /**
     * Helper method to get a random category for a given subcategory
     */
    private static String getRandomCategoryForSubcategory(String subcategoryId) {
        if (subcategoryId == null) return CATEGORY_TECH;
        
        switch (subcategoryId.toLowerCase()) {
            case SUBCATEGORY_ANDROID:
            case SUBCATEGORY_IOS:
            case SUBCATEGORY_WEB:
            case SUBCATEGORY_AI:
                return CATEGORY_TECH;
            case SUBCATEGORY_FITNESS:
            case SUBCATEGORY_NUTRITION:
            case SUBCATEGORY_MENTAL_HEALTH:
                return CATEGORY_HEALTH;
            case SUBCATEGORY_TRAVEL:
            case SUBCATEGORY_FOOD:
            case SUBCATEGORY_FASHION:
                return CATEGORY_LIFESTYLE;
            case SUBCATEGORY_FINANCE:
            case SUBCATEGORY_ECONOMY:
                return CATEGORY_BUSINESS;
            case SUBCATEGORY_FOOTBALL:
            case SUBCATEGORY_BASKETBALL:
            case SUBCATEGORY_TENNIS:
                return CATEGORY_SPORTS;
            case SUBCATEGORY_WORLD:
            case SUBCATEGORY_POLITICS:
                return CATEGORY_NEWS;
            default:
                return CATEGORY_TECH;
        }
    }

    /**
     * Get articles filtered by category ID, trying the API first and falling back to dummy data
     *
     * @param categoryId The category ID to filter by
     * @param callback   Callback to receive the results asynchronously
     */
    public void getArticlesByCategory(String categoryId, DataLoadListener callback) {
        if (!isViewModelInitialized) {
            // Fall back to dummy data if ViewModel isn't initialized
            List<Article> dummyArticles = getDummyArticlesByCategory(categoryId);
            if (callback != null) {
                callback.onDataLoaded(dummyArticles);
            }
            return;
        }

        // Try to get data from the API first
        articleViewModel.loadArticles(categoryId, 1, 20); // Default page 1, 20 items per page
        articleViewModel.getArticlesLiveData().observeForever(new Observer<List<Article>>() {
            @Override
            public void onChanged(List<Article> articles) {
                if (articles != null && !articles.isEmpty()) {
                    if (callback != null) {
                        callback.onDataLoaded(articles);
                    }
                    articleViewModel.getArticlesLiveData().removeObserver(this);
                } else {
                    // Fall back to dummy data if API returns empty
                    List<Article> dummyArticles = getDummyArticlesByCategory(categoryId);
                    if (callback != null) {
                        callback.onDataLoaded(dummyArticles);
                    }
                    articleViewModel.getArticlesLiveData().removeObserver(this);
                }
            }
        });

        // Set up error handling
        articleViewModel.getErrorLiveData().observeForever(new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null) {
                    // On error, fall back to dummy data
                    List<Article> dummyArticles = getDummyArticlesByCategory(categoryId);
                    if (callback != null) {
                        callback.onDataLoaded(dummyArticles);
                    }
                    articleViewModel.getErrorLiveData().removeObserver(this);
                }
            }
        });
    }

    /**
     * Get articles filtered by category ID (dummy data fallback)
     */
    public static List<Article> getDummyArticlesByCategory(String categoryId) {
        List<Article> filtered = new ArrayList<>();
        if (categoryId == null) {
            return filtered; // Return empty list if categoryId is null
        }
        
        for (Article article : getDummyArticles()) {
            String articleCategoryId = article.getCategoryId();
            if (articleCategoryId != null && categoryId.equalsIgnoreCase(articleCategoryId)) {
                filtered.add(article);
            }
        }
        return filtered;
    }

    /**
     * Get a single article by ID, trying the API first and falling back to dummy data
     *
     * @param id       The article ID to fetch
     * @param callback Callback to receive the result asynchronously
     */
    public void getArticleById(String id, SingleArticleCallback callback) {
        if (!isViewModelInitialized) {
            // Fall back to dummy data if ViewModel isn't initialized
            Article dummyArticle = getDummyArticleById(id);
            if (callback != null) {
                callback.onArticleLoaded(dummyArticle);
            }
            return;
        }

        // Try to get data from the API first
        articleViewModel.loadArticleById(id);
        articleViewModel.getArticleLiveData().observeForever(new Observer<Article>() {
            @Override
            public void onChanged(Article article) {
                if (article != null) {
                    if (callback != null) {
                        callback.onArticleLoaded(article);
                    }
                    articleViewModel.getArticleLiveData().removeObserver(this);
                } else {
                    // Fall back to dummy data if API returns null
                    Article dummyArticle = getDummyArticleById(id);
                    if (callback != null) {
                        callback.onArticleLoaded(dummyArticle);
                    }
                    articleViewModel.getArticleLiveData().removeObserver(this);
                }
            }
        });

        // Set up error handling
        articleViewModel.getErrorLiveData().observeForever(new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null) {
                    // On error, fall back to dummy data
                    Article dummyArticle = getDummyArticleById(id);
                    if (callback != null) {
                        callback.onArticleLoaded(dummyArticle);
                    }
                    articleViewModel.getErrorLiveData().removeObserver(this);
                }
            }
        });
    }

    /**
     * Get a single article by ID from dummy data (fallback method)
     */
    private Article getDummyArticleById(String id) {
        for (Article article : getDummyArticles()) {
            if (id.equals(article.getId())) {
                return article;
            }
        }
        return null;
    }


    /**
     * Get articles by author ID
     */
    public static List<Article> getDummyArticlesByAuthor(String authorId) {
        List<Article> filtered = new ArrayList<>();
        for (Article article : getDummyArticles()) {
            if (authorId.equals(article.getAuthorId())) {
                filtered.add(article);
            }
        }
        return filtered;
    }

    /**
     * Get the most viewed articles
     *
     * @param limit Maximum number of articles to return
     */
    public static List<Article> getMostViewedArticles(int limit) {
        List<Article> articles = new ArrayList<>(getDummyArticles());
        articles.sort((a1, a2) -> Integer.compare(a2.getViewCount(), a1.getViewCount()));
        return articles.subList(0, Math.min(limit, articles.size()));
    }

    /**
     * Get the most liked articles
     *
     * @param limit Maximum number of articles to return
     */
    public static List<Article> getMostLikedArticles(int limit) {
        List<Article> articles = new ArrayList<>(getDummyArticles());
        articles.sort((a1, a2) -> Integer.compare(a2.getLikeCount(), a1.getLikeCount()));
        return articles.subList(0, Math.min(limit, articles.size()));
    }

    /**
     * Get the newest articles
     *
     * @param limit Maximum number of articles to return
     */
    public static List<Article> getNewestArticles(int limit) {
        List<Article> articles = new ArrayList<>(getDummyArticles());
        articles.sort((a1, a2) -> a2.getPublishDate().compareTo(a1.getPublishDate()));
        return articles.subList(0, Math.min(limit, articles.size()));
    }

    /**
     * Get articles that user has started reading but not completed (continue reading)
     *
     * @param context  The context needed for ReadingProgressManager
     * @param limit    Maximum number of articles to return
     * @return List of articles in progress, sorted by last read time
     */
    public static List<Article> getContinueReadingArticles(Context context, int limit) {
        List<Article> allArticles = getDummyArticles();

        // Initialize ReadingProgressManager if not already done
        try {
            com.rafdi.vitechasia.blog.utils.ReadingProgressManager.initialize(context.getApplicationContext());
            com.rafdi.vitechasia.blog.utils.ReadingProgressManager readingProgressManager =
                com.rafdi.vitechasia.blog.utils.ReadingProgressManager.getInstance();

            return readingProgressManager.getContinueReadingArticles(allArticles)
                .stream()
                .limit(limit)
                .collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
        } catch (Exception e) {
            // If ReadingProgressManager fails, return empty list
            return new ArrayList<>();
        }
    }

    /**
     * Search for articles that match the given query in title, content, or author name
     *
     * @param query The search query
     * @return List of matching articles
     */
    public static List<Article> searchArticles(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String searchQuery = query.toLowerCase().trim();
        List<Article> allArticles = getDummyArticles();
        List<Article> results = new ArrayList<>();

        for (Article article : allArticles) {
            if ((article.getTitle() != null && article.getTitle().toLowerCase().contains(searchQuery)) ||
                    (article.getContent() != null && article.getContent().toLowerCase().contains(searchQuery)) ||
                    (article.getAuthorName() != null && article.getAuthorName().toLowerCase().contains(searchQuery))) {
                results.add(article);
            }
        }

        return results;
    }

    /**
     * Get all available categories with their subcategories
     */
    public static List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();

        // Technology
        List<String> techSubcategories = Arrays.asList(SUBCATEGORY_ANDROID, SUBCATEGORY_IOS, SUBCATEGORY_WEB, SUBCATEGORY_AI);
        categories.add(new Category(CATEGORY_TECH, "Technology", techSubcategories));

        // Health
        List<String> healthSubcategories = Arrays.asList(SUBCATEGORY_FITNESS, SUBCATEGORY_NUTRITION, SUBCATEGORY_MENTAL_HEALTH);
        categories.add(new Category(CATEGORY_HEALTH, "Health", healthSubcategories));

        // Lifestyle
        List<String> lifestyleSubcategories = Arrays.asList(SUBCATEGORY_TRAVEL, SUBCATEGORY_FOOD,
                SUBCATEGORY_FASHION);
        categories.add(new Category(CATEGORY_LIFESTYLE, "Lifestyle", lifestyleSubcategories));

        // Business
        List<String> businessSubcategories = Arrays.asList(SUBCATEGORY_FINANCE, SUBCATEGORY_ECONOMY);
        categories.add(new Category(CATEGORY_BUSINESS, "Business", businessSubcategories));

        // Sports
        List<String> sportsSubcategories = Arrays.asList(SUBCATEGORY_FOOTBALL, SUBCATEGORY_BASKETBALL, SUBCATEGORY_TENNIS);
        categories.add(new Category(CATEGORY_SPORTS, "Sports", sportsSubcategories));

        // News
        List<String> newsSubcategories = Arrays.asList(SUBCATEGORY_WORLD, SUBCATEGORY_POLITICS, SUBCATEGORY_ECONOMY);
        categories.add(new Category(CATEGORY_NEWS, "News", newsSubcategories));

        return categories;
    }

    /**
     * Get subcategories for a specific category
     */
    public static List<String> getSubcategoriesForCategory(String categoryName) {
        switch (categoryName.toLowerCase()) {
            case CATEGORY_TECH:
                return Arrays.asList(SUBCATEGORY_ANDROID, SUBCATEGORY_IOS, SUBCATEGORY_WEB, SUBCATEGORY_AI);
            case CATEGORY_HEALTH:
                return Arrays.asList(SUBCATEGORY_FITNESS, SUBCATEGORY_NUTRITION, SUBCATEGORY_MENTAL_HEALTH);
            case CATEGORY_LIFESTYLE:
                return Arrays.asList(SUBCATEGORY_TRAVEL, SUBCATEGORY_FOOD, SUBCATEGORY_FASHION);
            case CATEGORY_BUSINESS:
                return Arrays.asList(SUBCATEGORY_FINANCE, SUBCATEGORY_ECONOMY);
            case CATEGORY_SPORTS:
                return Arrays.asList(SUBCATEGORY_FOOTBALL, SUBCATEGORY_BASKETBALL, SUBCATEGORY_TENNIS);
            case CATEGORY_NEWS:
                return Arrays.asList(SUBCATEGORY_WORLD, SUBCATEGORY_POLITICS, SUBCATEGORY_ECONOMY);
            default:
                return new ArrayList<>();
        }
    }

    private static List<Article> generateDummyArticles() {
        List<Article> articles = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // =========================
        // TECH - ANDROID
        // =========================
        articles.add(createArticle(
                "tech1",
                "The Future of AI in Android Development",
                "Pengembangan Android sedang memasuki fase baru seiring semakin kuatnya kemampuan kecerdasan buatan yang berjalan langsung di perangkat. Pendekatan ini mengurangi ketergantungan pada layanan cloud dan memungkinkan fitur-fitur pintar bekerja secara lebih cepat dan responsif.\n\n" +
                        "Model AI yang berjalan secara lokal kini digunakan untuk berbagai fungsi seperti prediksi teks, pengelolaan daya adaptif, pengenalan suara, dan terjemahan instan. Hal ini meningkatkan pengalaman pengguna sekaligus menjaga privasi data karena pemrosesan dilakukan langsung di perangkat.\n\n" +
                        "Google terus mendorong adopsi AI melalui alat seperti TensorFlow Lite dan ML Kit. Dengan tooling yang semakin matang, pengembang dari berbagai skala dapat dengan mudah mengintegrasikan fitur AI tanpa memerlukan infrastruktur kompleks.\n\n" +
                        "Dari sisi privasi, pendekatan on-device AI menjadi solusi yang sejalan dengan meningkatnya kesadaran pengguna terhadap keamanan data. Informasi sensitif tidak perlu dikirim ke server eksternal, sehingga risiko kebocoran data dapat diminimalkan.\n\n" +
                        "Ke depan, AI diperkirakan akan menjadi bagian inti dari sistem Android, memengaruhi desain antarmuka, performa aplikasi, dan interaksi pengguna secara keseluruhan.",
                "Alex Johnson",
                "https://images.pexels.com/photos/1092644/pexels-photo-1092644.jpeg?cs=srgb&dl=pexels-fotios-photos-1092644.jpg&fm=jpg",
                calendar.getTime(),
                1250,
                CATEGORY_TECH,
                SUBCATEGORY_ANDROID
        ));

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        articles.add(createArticle(
                "tech2",
                "Android 14: New Features and Updates",
                "Android 14 menghadirkan serangkaian penyempurnaan yang berfokus pada stabilitas, efisiensi, dan kontrol pengguna. Alih-alih perubahan visual besar, versi ini menitikberatkan pada pengalaman penggunaan yang lebih halus dan konsisten.\n\n" +
                        "Optimalisasi daya menjadi salah satu sorotan utama. Sistem kini lebih cerdas dalam membatasi aktivitas latar belakang, membantu memperpanjang masa pakai baterai di berbagai jenis perangkat.\n\n" +
                        "Dari sisi privasi, Android 14 menawarkan transparansi yang lebih baik terkait izin aplikasi. Pengguna dapat memahami dengan jelas bagaimana data mereka digunakan dan memiliki kontrol lebih besar terhadap akses aplikasi.\n\n" +
                        "Bagi pengembang, pembaruan API mempermudah penyesuaian aplikasi dengan standar terbaru. Alat pengujian yang lebih baik juga membantu mengurangi bug sebelum rilis.\n\n" +
                        "Secara keseluruhan, Android 14 memperkuat fondasi platform dengan fokus pada keandalan jangka panjang dan kepercayaan pengguna.",
                "Maria Garcia",
                "https://sc0.blr1.cdn.digitaloceanspaces.com/article/127664-csdgcybqaf-1568959931.jpg",
                calendar.getTime(),
                980,
                CATEGORY_TECH,
                SUBCATEGORY_ANDROID
        ));

        // =========================
        // TECH - IOS
        // =========================
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        articles.add(createArticle(
                "tech3",
                "iOS 17: What's New for Developers",
                "iOS 17 membawa berbagai peningkatan yang ditujukan untuk mempermudah alur kerja pengembang. Alat debugging yang lebih canggih memungkinkan identifikasi masalah sejak tahap awal pengembangan.\n\n" +
                        "Apple juga memperluas dukungan aksesibilitas, membantu pengembang menciptakan aplikasi yang lebih inklusif. Peningkatan pengelolaan media memungkinkan pengalaman visual yang lebih kaya tanpa mengorbankan performa.\n\n" +
                        "Privasi tetap menjadi prioritas utama. iOS 17 memberikan transparansi lebih besar terkait aktivitas latar belakang aplikasi, meningkatkan kepercayaan pengguna.\n\n" +
                        "Integrasi yang lebih baik antara Swift dan Xcode mempercepat proses build dan iterasi. Hal ini membantu tim mengembangkan aplikasi dengan lebih efisien.\n\n" +
                        "Pendekatan Apple yang konsisten menjadikan iOS 17 sebagai pembaruan yang solid dan berorientasi jangka panjang.",
                "James Wilson",
                "https://media.istockphoto.com/id/1364620309/photo/iphone-13-pro.jpg?s=612x612&w=0&k=20&c=2h5Q46wh-eRyPwh4KKnJhCKFWqcd2ltgv9tdaULDdbc=",
                calendar.getTime(),
                1100,
                CATEGORY_TECH,
                SUBCATEGORY_IOS
        ));

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        articles.add(createArticle(
                "tech4",
                "Building Your First SwiftUI App",
                "SwiftUI mengubah cara pengembang membangun antarmuka aplikasi dengan pendekatan deklaratif. Pengembang cukup mendeskripsikan tampilan dan perilaku UI, sementara sistem menangani pembaruan secara otomatis.\n\n" +
                        "Bagi pemula, SwiftUI mengurangi kompleksitas karena minim boilerplate code. Tata letak secara otomatis menyesuaikan berbagai ukuran layar dan pengaturan aksesibilitas.\n\n" +
                        "Manajemen state menjadi konsep penting dalam SwiftUI. Dengan memahami alur data, pengembang dapat membangun antarmuka yang responsif dan mudah dirawat.\n\n" +
                        "Animasi dan transisi dapat diterapkan dengan sangat sederhana, memungkinkan pengalaman pengguna yang lebih menarik.\n\n" +
                        "SwiftUI kini menjadi pilihan utama untuk pengembangan aplikasi modern di ekosistem Apple.",
                "Sarah Chen",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQL5o2QWel7p5B_DZ3UmY_SDzwseR_VYA0MRA&s",
                calendar.getTime(),
                850,
                CATEGORY_TECH,
                SUBCATEGORY_IOS
        ));

        // =========================
        // TECH - WEB
        // =========================
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        articles.add(createArticle(
                "tech5",
                "Building Scalable Web Applications in 2023",
                "Skalabilitas menjadi tantangan utama dalam pengembangan aplikasi web modern. Arsitektur berbasis microservices dan serverless semakin populer untuk mengatasi lonjakan trafik.\n\n" +
                        "Strategi caching, observabilitas sistem, dan pipeline deployment otomatis berperan penting dalam menjaga performa aplikasi.\n\n" +
                        "Tim yang berinvestasi pada fondasi teknis yang kuat dapat beradaptasi lebih cepat terhadap perubahan kebutuhan pengguna.\n\n" +
                        "Pemanfaatan edge computing juga membantu mengurangi latensi dengan mendekatkan konten ke pengguna.\n\n" +
                        "Pendekatan ini memungkinkan aplikasi web tetap andal meskipun skala pengguna terus bertambah.",
                "David Kim",
                "https://www.businessofapps.com/wp-content/uploads/2019/11/dd_scalable_web_apps_cover.jpg",
                calendar.getTime(),
                1320,
                CATEGORY_TECH,
                SUBCATEGORY_WEB
        ));

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        articles.add(createArticle(
                "tech6",
                "React vs Vue vs Angular in 2023",
                "Framework frontend telah berkembang menjadi ekosistem yang stabil dan matang. React menawarkan komunitas besar, Vue dikenal ramah bagi pengembang, sementara Angular unggul untuk aplikasi skala besar.\n\n" +
                        "Pemilihan framework sering kali bergantung pada kebutuhan tim dan kompleksitas proyek.\n\n" +
                        "Alih-alih mengejar tren, fokus pada arsitektur yang jelas dan praktik pengujian yang baik akan menghasilkan aplikasi yang lebih tahan lama.\n\n" +
                        "Ketiga framework ini mampu menghasilkan aplikasi berkualitas jika digunakan dengan tepat.\n\n" +
                        "Keputusan terbaik adalah yang paling sesuai dengan konteks proyek dan keahlian tim.",
                "Emma Davis",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQyMx_A4T9rp-M_k5hn5d7rKuqUa3CPyOunNg&s",
                calendar.getTime(),
                1500,
                CATEGORY_TECH,
                SUBCATEGORY_WEB
        ));

        // =========================
        // TECH - AI
        // =========================
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        articles.add(createArticle(
                "tech7",
                "The Future of AI in Mobile Development",
                "AI pada perangkat mobile kini bergerak menuju pendekatan hybrid dan on-device. Hal ini memungkinkan fitur cerdas berjalan lebih cepat tanpa ketergantungan koneksi internet.\n\n" +
                        "Aplikasi dapat memanfaatkan pengenalan suara, kamera pintar, dan antarmuka adaptif secara real-time.\n\n" +
                        "Model yang lebih kecil dan teroptimasi memudahkan distribusi ke berbagai perangkat.\n\n" +
                        "Perkembangan ini membuka peluang inovasi baru di bidang aplikasi mobile.\n\n" +
                        "AI diprediksi akan menjadi standar dalam pengembangan aplikasi modern.",
                "Alex Johnson",
                "https://media.licdn.com/dms/image/v2/D4E12AQEOAPlMZuV8uA/article-cover_image-shrink_600_2000/article-cover_image-shrink_600_2000/0/1721198653325?e=2147483647&v=beta&t=sAzUoz6YdLUQgBkXLgaVBuYA_QWLWlcczCHji3Tigdo",
                calendar.getTime(),
                2100,
                CATEGORY_TECH,
                SUBCATEGORY_AI
        ));

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        articles.add(createArticle(
                "tech8",
                "Getting Started with Machine Learning",
                "Machine learning kini lebih mudah dipelajari berkat library tingkat tinggi dan model siap pakai.\n\n" +
                        "Pemula dapat memulai dengan konsep dasar seperti klasifikasi dan regresi.\n\n" +
                        "Persiapan data dan evaluasi model menjadi faktor penting dalam keberhasilan proyek.\n\n" +
                        "Pemahaman dasar ini membantu menghindari kesalahan umum seperti overfitting.\n\n" +
                        "Dengan pendekatan yang tepat, machine learning dapat diterapkan secara efektif.",
                "Robert Taylor",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQRbMXxQpsINp70uZ_XBBQ5jNqIdAi-CKYVSA&s",
                calendar.getTime(),
                1750,
                CATEGORY_TECH,
                SUBCATEGORY_AI
        ));

        // =========================
        // HEALTH
        // =========================
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        articles.add(createArticle(
                "health1",
                "10 Essential Exercises for Home Workouts",
                "Olahraga di rumah dapat tetap efektif dengan rutinitas yang seimbang.\n\n" +
                        "Latihan ini mencakup kekuatan, mobilitas, dan kardio tanpa peralatan khusus.\n\n" +
                        "Konsistensi lebih penting dibanding intensitas tinggi.\n\n" +
                        "Peningkatan bertahap membantu mencegah cedera.\n\n" +
                        "Gaya hidup aktif mendukung kesehatan jangka panjang.",
                "Dr. Michael Brown",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSfWgoLdMrUq7_YZyH8i17i6r2dnhNRaq5Mhw&s",
                calendar.getTime(),
                1530,
                CATEGORY_HEALTH,
                SUBCATEGORY_FITNESS
        ));

        calendar.add(Calendar.DAY_OF_MONTH, -2);
        articles.add(createArticle(
                "health2",
                "The Science of Intermittent Fasting",
                "Intermittent fasting menjadi populer sebagai metode pengelolaan berat badan.\n\n" +
                        "Penelitian menunjukkan manfaat terhadap sensitivitas insulin.\n\n" +
                        "Namun, metode ini tidak cocok untuk semua orang.\n\n" +
                        "Konsultasi medis sangat dianjurkan sebelum memulai.\n\n" +
                        "Pendekatan berkelanjutan adalah kunci keberhasilan.",
                "Dr. Emily Wilson",
                "https://www.leapstore.in/cdn/shop/articles/22_December_Fasting_Blog_Banner.png?v=1703662458",
                calendar.getTime(),
                2100,
                CATEGORY_HEALTH,
                SUBCATEGORY_NUTRITION
        ));

        // =========================
        // SPORTS
        // =========================
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        articles.add(createArticle(
                "sport1",
                "Local Team Wins Championship in Overtime Thriller",
                "Pertandingan final berlangsung dramatis hingga babak perpanjangan waktu.\n\n" +
                        "Gol penentuan kemenangan disambut sorak sorai penonton.\n\n" +
                        "Pelatih memuji kerja sama dan mental juara tim.\n\n" +
                        "Pemain cadangan memberikan kontribusi penting.\n\n" +
                        "Perayaan kemenangan direncanakan sepanjang akhir pekan.",
                "John Sportsman",
                "https://ocsportszone.com/wp-content/uploads/2025/11/DSC04382-Pano-1-1-768x383.jpg",
                calendar.getTime(),
                3200,
                CATEGORY_SPORTS,
                SUBCATEGORY_FOOTBALL
        ));

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        articles.add(createArticle(
                "sport2",
                "NBA Season Preview: Top Teams to Watch",
                "Musim NBA yang baru menghadirkan persaingan ketat.\n\n" +
                        "Perubahan roster meningkatkan ekspektasi penggemar.\n\n" +
                        "Pemain muda dan veteran akan menentukan hasil.\n\n" +
                        "Pertandingan awal musim menjadi indikator kekuatan tim.\n\n" +
                        "Persaingan konferensi diprediksi berlangsung sengit.",
                "Jane Hooper",
                "https://img.olympics.com/images/image/private/t_s_pog_staticContent_hero_xl_2x/f_auto/v1729070242/primary/inujhovgebftbhqvmqf0",
                calendar.getTime(),
                2750,
                CATEGORY_SPORTS,
                SUBCATEGORY_BASKETBALL
        ));

        // =========================
        // NEWS
        // =========================
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        articles.add(createArticle(
                "news1",
                "Global Leaders Sign Historic Climate Agreement",
                "Para pemimpin dunia menandatangani kesepakatan iklim bersejarah.\n\n" +
                        "Kesepakatan ini menargetkan pengurangan emisi secara agresif.\n\n" +
                        "Pendanaan untuk negara berkembang turut disepakati.\n\n" +
                        "Pemantauan progres akan dilakukan secara berkala.\n\n" +
                        "Aktivis mendorong implementasi yang lebih cepat.",
                "Global News Network",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRWjyDpCjZ7kyT5geshacLD0NcuaBaDpRNN7g&s",
                calendar.getTime(),
                4100,
                CATEGORY_NEWS,
                SUBCATEGORY_WORLD
        ));

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        articles.add(createArticle(
                "news2",
                "New Economic Policy Aims to Boost Local Businesses",
                "Pemerintah meluncurkan kebijakan ekonomi baru.\n\n" +
                        "Insentif pajak ditujukan untuk usaha kecil dan menengah.\n\n" +
                        "Program ini diharapkan menciptakan lapangan kerja.\n\n" +
                        "Ekonom menilai dampak jangka pendek cukup positif.\n\n" +
                        "Reformasi struktural tetap dibutuhkan untuk pertumbuhan berkelanjutan.",
                "Financial Times",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTwa5Bf3kdH7KzhnOmrNlWWPEkKM5VOr4TP8A&s",
                calendar.getTime(),
                1870,
                CATEGORY_NEWS,
                SUBCATEGORY_ECONOMY
        ));

        return articles;
    }


    /**
     * Gets popular articles, trying the API first and falling back to dummy data
     *
     * @param count    Maximum number of articles to return
     * @param callback Callback to receive the results asynchronously
     */
    public void getPopularArticles(int count, DataLoadListener callback) {
        // For this example, we'll just get all articles and sort by view count
        // In a real app, you might have a separate API endpoint for popular articles
        getAllArticles(new DataLoadListener() {
            @Override
            public void onDataLoaded(List<Article> articles) {
                if (articles != null) {
                    // Sort by view count (descending) and limit to count
                    articles.sort((a1, a2) -> Integer.compare(a2.getViewCount(), a1.getViewCount()));
                    List<Article> result = articles.subList(0, Math.min(count, articles.size()));
                    if (callback != null) {
                        callback.onDataLoaded(result);
                    }
                } else if (callback != null) {
                    callback.onError("Failed to load popular articles");
                }
            }

            @Override
            public void onError(String message) {
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }

    /**
     * Gets latest articles, trying the API first and falling back to dummy data
     *
     * @param count    Maximum number of articles to return
     * @param callback Callback to receive the results asynchronously
     */
    public void getLatestArticles(int count, DataLoadListener callback) {
        // For this example, we'll just get all articles and sort by date
        // In a real app, you might have a separate API endpoint for latest articles
        getAllArticles(new DataLoadListener() {
            @Override
            public void onDataLoaded(List<Article> articles) {
                if (articles != null) {
                    // Sort by publish date (newest first) and limit to count
                    articles.sort((a1, a2) -> a2.getPublishDate().compareTo(a1.getPublishDate()));
                    List<Article> result = articles.subList(0, Math.min(count, articles.size()));
                    if (callback != null) {
                        callback.onDataLoaded(result);
                    }
                } else if (callback != null) {
                    callback.onError("Failed to load latest articles");
                }
            }

            @Override
            public void onError(String message) {
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }
    /**
     * Gets all articles, trying the API first and falling back to dummy data
     *
     * @param callback Callback to receive the results asynchronously
     */
    public void getAllArticles(DataLoadListener callback) {
        if (!isViewModelInitialized) {
            // Fall back to dummy data if ViewModel isn't initialized
            List<Article> dummyArticles = getDummyAllArticles();
            if (callback != null) {
                callback.onDataLoaded(dummyArticles);
            }
            return;
        }

        // Try to get data from the API first
        articleViewModel.loadArticles(null, 1, 100); // Assuming 100 is a reasonable max
        articleViewModel.getArticlesLiveData().observeForever(new Observer<List<Article>>() {
            @Override
            public void onChanged(List<Article> articles) {
                if (articles != null && !articles.isEmpty()) {
                    if (callback != null) {
                        callback.onDataLoaded(articles);
                    }
                    articleViewModel.getArticlesLiveData().removeObserver(this);
                } else {
                    List<Article> dummyArticles = getDummyAllArticles();
                    if (callback != null) {
                        callback.onDataLoaded(dummyArticles);
                    }
                    articleViewModel.getArticlesLiveData().removeObserver(this);
                }
            }
        });

        // Set up error handling
        articleViewModel.getErrorLiveData().observeForever(new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null && callback != null) {
                    List<Article> dummyArticles = getDummyAllArticles();
                    callback.onDataLoaded(dummyArticles);
                    articleViewModel.getErrorLiveData().removeObserver(this);
                }
            }
        });
    }

    /**
     * Gets all articles from dummy data (fallback method)
     */
    private List<Article> getDummyAllArticles() {
        return getDummyArticles();
    }

    private static Article createArticle(String id, String title, String content, String authorName,
                                         String imageUrl, Date date, int viewCount,
                                         String categoryId, String subcategoryId) {
        Article article = new Article(
                id,
                title,
                content,
                imageUrl,
                categoryId,
                subcategoryId,
                "author_" + id,
                authorName,
                "https://example.com/authors/" + authorName.toLowerCase().replace(" ", "_") + ".jpg",
                date,
                viewCount,
                viewCount / 10
        );
        return article;
    }
}
import java.util.*;
import java.util.stream.Collectors;

/*
 * =====================================================================
 *   Young Book Lovers Dating Club — Design Patterns Demo
 * =====================================================================
 *  Patterns used:
 *   1. Singleton               — UserRepository (single shared user storage)
 *   2. Builder                 — User.Builder (step-by-step profile creation)
 *   3. Factory Method          — ContactChannel.createNotifier() + NotifierFactory registry
 *   4. Strategy                — MatchStrategy (matching algorithms)
 *                                ContactChannel (one class per contact channel)
 *   5. Observer                — ClubEventBus (subscriptions to club events)
 *   6. Decorator               — ProfileView (badges and reading stats on a profile)
 *   7. Adapter                 — LegacyCatalogAdapter (old library catalog)
 *   8. Chain of Responsibility — RegistrationValidator (registration checks)
 *   9. Facade                  — BookLoversClub (simple entry point to the system)
 * ---------------------------------------------------------------------
 *  MD1 Group A - refactoring a growing switch
 *   Before: NotifierFactory had a switch on the ContactChannel enum and
 *           ContactValidator had a check only for EMAIL. For every new
 *           channel we had to change these classes.
 *   After:  each channel is a separate class that implements the
 *           ContactChannel interface (Strategy). NotifierFactory keeps the
 *           channels in a map, so there is no switch. To add a channel we
 *           write a new class and register it (see part 7 of the demo).
 *   The old code is in before/Main.java.
 * =====================================================================
 */

// ============================== DEMO ================================

public class Main {
    public static void main(String[] args) {
        BookLoversClub club = new BookLoversClub();

        System.out.println("===== 1. Registration (Builder + Chain of Responsibility + Singleton) =====");
        User aigerim = new User.Builder("Aigerim", 20)
                .city("Almaty")
                .contact("TELEGRAM", "@aigerim_reads")
                .genres(Genre.FANTASY, Genre.CLASSIC, Genre.POETRY)
                .authors("Mikhail Bulgakov", "J.K. Rowling")
                .about("I love reading in coffee shops")
                .build();

        User arman = new User.Builder("Arman", 22)
                .city("Almaty")
                .contact("EMAIL", "arman@mail.kz")
                .genres(Genre.FANTASY, Genre.SCI_FI, Genre.CLASSIC)
                .authors("Frank Herbert", "Mikhail Bulgakov")
                .build();

        User dana = new User.Builder("Dana", 19)
                .city("Astana")
                .contact("SMS", "+7 701 000 00 00")
                .genres(Genre.DETECTIVE, Genre.ROMANCE)
                .authors("Agatha Christie")
                .build();

        User timur = new User.Builder("Timur", 24)
                .city("Almaty")
                .contact("TELEGRAM", "@timur_books")
                .genres(Genre.NON_FICTION, Genre.SCI_FI, Genre.CLASSIC)
                .authors("Yuval Noah Harari", "Erich Maria Remarque")
                .build();

        User tooOld = new User.Builder("Victor", 45)
                .contact("EMAIL", "victor@mail.kz")
                .genres(Genre.CLASSIC)
                .build();

        User noGenres = new User.Builder("Olzhas", 18)
                .contact("EMAIL", "olzhas@mail.kz")
                .build();

        club.register(aigerim);
        club.register(arman);
        club.register(dana);
        club.register(timur);
        club.register(tooOld);
        club.register(noGenres);
        System.out.println("Total members: " + club.membersCount()
                + " (Singleton: same repository instance? "
                + (UserRepository.getInstance() == UserRepository.getInstance()) + ")");

        System.out.println("\n===== 2. Books read (Adapter) =====");
        club.addReadBook(aigerim, "978-5-17-080115-2");
        club.addReadBook(aigerim, "978-5-389-07435-4");
        club.addReadBook(aigerim, "978-5-389-01006-2");
        club.addReadBook(arman, "978-5-17-090630-7");
        club.addReadBook(arman, "978-5-17-080115-2");
        club.addReadBook(dana, "978-5-699-12014-7");
        club.addReadBook(timur, "978-5-04-116500-3");
        club.addReadBook(timur, "978-5-17-118366-0");
        club.addReadBook(timur, "000-0-00-000000-0");

        System.out.println("\n===== 3. Profiles (Decorator) =====");
        club.verify(aigerim);
        System.out.println(club.showProfile(aigerim));
        System.out.println(club.showProfile(timur));

        System.out.println("\n===== 4. Finding matches for Aigerim (Strategy) =====");
        List<MatchStrategy> strategies = Arrays.asList(
                new GenreMatchStrategy(),
                new AuthorMatchStrategy(),
                new AgeCityMatchStrategy(),
                new WeightedMatchStrategy()
                        .add(new GenreMatchStrategy(), 0.4)
                        .add(new AuthorMatchStrategy(), 0.4)
                        .add(new AgeCityMatchStrategy(), 0.2)
        );
        for (MatchStrategy s : strategies) {
            System.out.println("Strategy: " + s.name());
            List<Match> matches = club.findMatches(aigerim, s, 3);
            if (matches.isEmpty()) System.out.println("   no matches");
            matches.forEach(m -> System.out.println("   " + m));
        }

        System.out.println("\n===== 5. Introduction (Observer + Factory Method) =====");
        MatchStrategy best = strategies.get(strategies.size() - 1);
        List<Match> top = club.findMatches(aigerim, best, 1);
        if (!top.isEmpty()) {
            club.introduce(aigerim, top.get(0).user, best);
        }

        System.out.println("\n===== 6. Book club meetup (Observer) =====");
        club.announceMeetup("The Master and Margarita", "Bookworm Cafe, Almaty", "October 12, 6:00 PM");

        System.out.println("\n===== 7. New channel WhatsApp, old code not changed (Strategy + Factory) =====");
        System.out.println("Channels now: " + club.contactChannels());
        try {
            new User.Builder("Madina", 21).contact("WHATSAPP", "+77071234567");
        } catch (IllegalArgumentException e) {
            System.out.println("   Before registering: " + e.getMessage());
        }

        // we only register the new channel here, no old class is changed
        club.addContactChannel(new WhatsAppChannel());
        System.out.println("Channels now: " + club.contactChannels());

        User madina = new User.Builder("Madina", 21)
                .city("Almaty")
                .contact("WHATSAPP", "+77071234567")
                .genres(Genre.CLASSIC, Genre.POETRY)
                .authors("Abai Kunanbaev")
                .build();
        User ruslan = new User.Builder("Ruslan", 23)
                .contact("WHATSAPP", "my number")
                .genres(Genre.SCI_FI)
                .build();
        club.register(madina);   // ok
        club.register(ruslan);   // wrong number, WhatsAppChannel does not accept it
        System.out.println("Meetup message (Madina gets it on WhatsApp):");
        club.announceMeetup("The Little Prince", "Bookworm Cafe, Almaty", "October 19, 6:00 PM");
    }
}

// ============================== MODEL ===============================

enum Genre { FANTASY, SCI_FI, DETECTIVE, CLASSIC, ROMANCE, NON_FICTION, POETRY }

class Book {
    private final String title;
    private final String author;
    private final Genre genre;

    Book(String title, String author, Genre genre) {
        this.title = title;
        this.author = author;
        this.genre = genre;
    }

    String getTitle()  { return title; }
    String getAuthor() { return author; }
    Genre getGenre()   { return genre; }

    @Override
    public String toString() { return "\"" + title + "\" by " + author + " (" + genre + ")"; }
}

// ========================== 2. BUILDER ==============================

class User {
    private final String id;
    private final String name;
    private final int age;
    private final String city;
    private final String contact;
    private final ContactChannel channel;
    private final String about;
    private final Set<Genre> favoriteGenres;
    private final Set<String> favoriteAuthors;
    private final List<Book> readBooks;
    private boolean verified;

    private User(Builder b) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = b.name;
        this.age = b.age;
        this.city = b.city;
        this.contact = b.contact;
        this.channel = b.channel;
        this.about = b.about;
        this.favoriteGenres = b.favoriteGenres;
        this.favoriteAuthors = b.favoriteAuthors;
        this.readBooks = new ArrayList<>();
    }

    String getId()                   { return id; }
    String getName()                 { return name; }
    int getAge()                     { return age; }
    String getCity()                 { return city; }
    String getContact()              { return contact; }
    ContactChannel getChannel()      { return channel; }
    String getAbout()                { return about; }
    Set<Genre> getFavoriteGenres()   { return favoriteGenres; }
    Set<String> getFavoriteAuthors() { return favoriteAuthors; }
    List<Book> getReadBooks()        { return readBooks; }
    boolean isVerified()             { return verified; }
    void setVerified(boolean v)      { this.verified = v; }
    void addBook(Book book)          { readBooks.add(book); }

    @Override
    public String toString() { return name + " (" + age + ", " + city + ")"; }

    static class Builder {
        private final String name;
        private final int age;
        private String city = "Not specified";
        private String contact = "";
        private ContactChannel channel = NotifierFactory.channel("EMAIL");
        private String about = "";
        private final Set<Genre> favoriteGenres = new HashSet<>();
        private final Set<String> favoriteAuthors = new HashSet<>();

        Builder(String name, int age) {
            this.name = name;
            this.age = age;
        }

        Builder city(String city)                    { this.city = city; return this; }
        Builder contact(ContactChannel ch, String c) { this.channel = ch; this.contact = c; return this; }
        /** Same as above, but finds the channel by name, for example "TELEGRAM". */
        Builder contact(String channelCode, String c) { return contact(NotifierFactory.channel(channelCode), c); }
        Builder about(String about)                  { this.about = about; return this; }
        Builder genres(Genre... genres)              { favoriteGenres.addAll(Arrays.asList(genres)); return this; }
        Builder authors(String... authors)           { favoriteAuthors.addAll(Arrays.asList(authors)); return this; }

        User build() { return new User(this); }
    }
}

// ========================= 1. SINGLETON =============================

class UserRepository {
    private static volatile UserRepository instance;
    private final Map<String, User> users = new LinkedHashMap<>();

    private UserRepository() { }

    /** Thread-safe lazy initialization (double-checked locking). */
    static UserRepository getInstance() {
        if (instance == null) {
            synchronized (UserRepository.class) {
                if (instance == null) {
                    instance = new UserRepository();
                }
            }
        }
        return instance;
    }

    void save(User user)               { users.put(user.getId(), user); }
    Optional<User> findById(String id) { return Optional.ofNullable(users.get(id)); }
    Collection<User> findAll()         { return Collections.unmodifiableCollection(users.values()); }
    int count()                        { return users.size(); }
}

// ================== 8. CHAIN OF RESPONSIBILITY ======================

abstract class RegistrationValidator {
    private RegistrationValidator next;

    RegistrationValidator linkWith(RegistrationValidator next) {
        this.next = next;
        return next;
    }

    /** Returns null if the user is valid, otherwise an error message. */
    String validate(User user) {
        String error = check(user);
        if (error != null) return error;
        return next == null ? null : next.validate(user);
    }

    protected abstract String check(User user);
}

class NameValidator extends RegistrationValidator {
    @Override
    protected String check(User u) {
        return (u.getName() == null || u.getName().trim().length() < 2)
                ? "Name must be at least 2 characters long" : null;
    }
}

class AgeValidator extends RegistrationValidator {
    static final int MIN_AGE = 14;
    static final int MAX_AGE = 30;

    @Override
    protected String check(User u) {
        return (u.getAge() < MIN_AGE || u.getAge() > MAX_AGE)
                ? "The club is for young people aged " + MIN_AGE + " to " + MAX_AGE : null;
    }
}

class GenresValidator extends RegistrationValidator {
    @Override
    protected String check(User u) {
        return u.getFavoriteGenres().isEmpty()
                ? "Please choose at least one favorite genre" : null;
    }
}

class ContactValidator extends RegistrationValidator {
    @Override
    protected String check(User u) {
        String c = u.getContact();
        if (c == null || c.isEmpty()) return "Please provide a contact for notifications";
        // no check for EMAIL here now, every channel checks its own contact
        return u.getChannel().checkContact(c);
    }
}

// ============ 3. FACTORY METHOD + STRATEGY (changed in MD1) ===============

interface Notifier {
    void send(User to, String message);
}

class EmailNotifier implements Notifier {
    public void send(User to, String message) {
        System.out.println("   [E-mail -> " + to.getContact() + "] " + message);
    }
}

class TelegramNotifier implements Notifier {
    public void send(User to, String message) {
        System.out.println("   [Telegram -> " + to.getContact() + "] " + message);
    }
}

class SmsNotifier implements Notifier {
    public void send(User to, String message) {
        System.out.println("   [SMS -> " + to.getContact() + "] " + message);
    }
}

/**
 * Strategy pattern. Every contact channel is a class that implements this
 * interface. All things that are different for each channel are inside the
 * channel class, so other classes don't need a switch or "if EMAIL".
 */
interface ContactChannel {
    /** Name of the channel, for example "EMAIL". We use it as the key in the map. */
    String code();

    /** Factory Method: the channel creates its own Notifier. */
    Notifier createNotifier();

    /** Checks the contact for this channel. Returns null if it is ok, or an error text. */
    String checkContact(String contact);
}

class EmailChannel implements ContactChannel {
    public String code()              { return "EMAIL"; }
    public Notifier createNotifier()  { return new EmailNotifier(); }
    public String checkContact(String c) { return c.contains("@") ? null : "Invalid e-mail address"; }
}

class TelegramChannel implements ContactChannel {
    public String code()              { return "TELEGRAM"; }
    public Notifier createNotifier()  { return new TelegramNotifier(); }
    public String checkContact(String c) { return null; }   // no extra rule, same as before
}

class SmsChannel implements ContactChannel {
    public String code()              { return "SMS"; }
    public Notifier createNotifier()  { return new SmsNotifier(); }
    public String checkContact(String c) { return null; }   // no extra rule, same as before
}

/**
 * Factory with a map of channels instead of the old switch.
 * The factory finds a channel by its name. New channels are added with
 * register(), so we don't need to change this class again.
 */
final class NotifierFactory {
    private static final Map<String, ContactChannel> CHANNELS = new LinkedHashMap<>();

    static {    // channels that the club has from the start
        register(new EmailChannel());
        register(new TelegramChannel());
        register(new SmsChannel());
    }

    private NotifierFactory() { }

    static void register(ContactChannel channel) {
        CHANNELS.put(channel.code().toUpperCase(Locale.ROOT), channel);
    }

    static ContactChannel channel(String code) {
        ContactChannel channel = CHANNELS.get(code.toUpperCase(Locale.ROOT));
        if (channel == null) {
            throw new IllegalArgumentException("Unknown contact channel \"" + code
                    + "\". Registered channels: " + CHANNELS.keySet());
        }
        return channel;
    }

    static Set<String> registeredCodes() { return Collections.unmodifiableSet(CHANNELS.keySet()); }

    /** Same method as before, so UserSubscriber still works without changes. */
    static Notifier create(ContactChannel channel) {
        return channel.createNotifier();
    }
}

// ========================== 5. OBSERVER =============================

interface ClubEventListener {
    void onEvent(String eventType, String message);
}

/** A user-subscriber receives events through their own contact channel. */
class UserSubscriber implements ClubEventListener {
    private final User user;
    private final Notifier notifier;

    UserSubscriber(User user) {
        this.user = user;
        this.notifier = NotifierFactory.create(user.getChannel()); // Factory used inside Observer
    }

    @Override
    public void onEvent(String eventType, String message) {
        notifier.send(user, "[" + eventType + "] " + message);
    }
}

class ClubEventBus {
    private final Map<String, List<ClubEventListener>> listeners = new HashMap<>();

    void subscribe(String eventType, ClubEventListener l) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(l);
    }

    void unsubscribe(String eventType, ClubEventListener l) {
        List<ClubEventListener> list = listeners.get(eventType);
        if (list != null) list.remove(l);
    }

    void publish(String eventType, String message) {
        for (ClubEventListener l : listeners.getOrDefault(eventType, Collections.emptyList())) {
            l.onEvent(eventType, message);
        }
    }
}

// ========================== 4. STRATEGY =============================

interface MatchStrategy {
    /** Compatibility from 0.0 to 1.0 */
    double score(User a, User b);
    String name();
}

/** Similarity by favorite genres (Jaccard index). */
class GenreMatchStrategy implements MatchStrategy {
    public double score(User a, User b) {
        return jaccard(a.getFavoriteGenres(), b.getFavoriteGenres());
    }
    public String name() { return "By genres"; }

    static <T> double jaccard(Set<T> x, Set<T> y) {
        if (x.isEmpty() && y.isEmpty()) return 0;
        Set<T> intersection = new HashSet<>(x);
        intersection.retainAll(y);
        Set<T> union = new HashSet<>(x);
        union.addAll(y);
        return (double) intersection.size() / union.size();
    }
}

/** Similarity by favorite authors and authors of books already read. */
class AuthorMatchStrategy implements MatchStrategy {
    public double score(User a, User b) {
        Set<String> authorsA = new HashSet<>(a.getFavoriteAuthors());
        Set<String> authorsB = new HashSet<>(b.getFavoriteAuthors());
        a.getReadBooks().forEach(book -> authorsA.add(book.getAuthor()));
        b.getReadBooks().forEach(book -> authorsB.add(book.getAuthor()));
        return GenreMatchStrategy.jaccard(authorsA, authorsB);
    }
    public String name() { return "By authors"; }
}

/** Closeness in age and living in the same city. */
class AgeCityMatchStrategy implements MatchStrategy {
    public double score(User a, User b) {
        double ageScore = Math.max(0, 1 - Math.abs(a.getAge() - b.getAge()) / 8.0);
        double cityScore = a.getCity().equalsIgnoreCase(b.getCity()) ? 1.0 : 0.0;
        return 0.5 * ageScore + 0.5 * cityScore;
    }
    public String name() { return "By age and city"; }
}

/** Combined strategy with weights. */
class WeightedMatchStrategy implements MatchStrategy {
    private final Map<MatchStrategy, Double> weights = new LinkedHashMap<>();

    WeightedMatchStrategy add(MatchStrategy s, double weight) {
        weights.put(s, weight);
        return this;
    }

    public double score(User a, User b) {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) return 0;
        double sum = 0;
        for (Map.Entry<MatchStrategy, Double> e : weights.entrySet()) {
            sum += e.getKey().score(a, b) * e.getValue();
        }
        return sum / total;
    }
    public String name() { return "Weighted (combined)"; }
}

// ========================== 6. DECORATOR ============================

interface ProfileView {
    String render();
}

class BasicProfileView implements ProfileView {
    private final User user;
    BasicProfileView(User user) { this.user = user; }

    public String render() {
        return "👤 " + user.getName() + ", " + user.getAge() + " y.o., " + user.getCity()
                + "\n   Genres: " + user.getFavoriteGenres()
                + "\n   Authors: " + user.getFavoriteAuthors()
                + (user.getAbout().isEmpty() ? "" : "\n   About: " + user.getAbout());
    }
}

abstract class ProfileDecorator implements ProfileView {
    protected final ProfileView inner;
    ProfileDecorator(ProfileView inner) { this.inner = inner; }
}

class VerifiedBadgeDecorator extends ProfileDecorator {
    VerifiedBadgeDecorator(ProfileView inner) { super(inner); }
    public String render() { return inner.render() + "\n   ✅ Verified profile"; }
}

class ReadingStatsDecorator extends ProfileDecorator {
    private final User user;
    ReadingStatsDecorator(ProfileView inner, User user) { super(inner); this.user = user; }

    public String render() {
        int count = user.getReadBooks().size();
        String level = count >= 5 ? "Bookworm 🐛" : count >= 2 ? "Reader 📖" : "Newbie 🌱";
        return inner.render() + "\n   📚 Books read: " + count + " — " + level;
    }
}

class LastBookDecorator extends ProfileDecorator {
    private final User user;
    LastBookDecorator(ProfileView inner, User user) { super(inner); this.user = user; }

    public String render() {
        List<Book> books = user.getReadBooks();
        if (books.isEmpty()) return inner.render();
        return inner.render() + "\n   🔖 Last book: " + books.get(books.size() - 1);
    }
}

// =========================== 7. ADAPTER =============================

/** The interface our system expects. */
interface BookCatalog {
    Optional<Book> findByIsbn(String isbn);
}

/** Old city library catalog with an inconvenient API (cannot be modified). */
class LegacyLibraryCatalog {
    private final Map<String, String> records = new HashMap<>();

    LegacyLibraryCatalog() {
        records.put("978-5-17-080115-2", "The Master and Margarita|Mikhail Bulgakov|CLS");
        records.put("978-5-389-07435-4", "Harry Potter and the Philosopher's Stone|J.K. Rowling|FNT");
        records.put("978-5-17-090630-7", "Dune|Frank Herbert|SCF");
        records.put("978-5-699-12014-7", "Murder on the Orient Express|Agatha Christie|DET");
        records.put("978-5-17-118366-0", "Three Comrades|Erich Maria Remarque|CLS");
        records.put("978-5-389-01006-2", "The Little Prince|Antoine de Saint-Exupery|CLS");
        records.put("978-5-04-116500-3", "Sapiens|Yuval Noah Harari|NFC");
    }

    /** Returns a string in the format "title|author|CODE", or null. */
    String lookupRecord(String code) { return records.get(code); }
}

class LegacyCatalogAdapter implements BookCatalog {
    private final LegacyLibraryCatalog legacy;

    LegacyCatalogAdapter(LegacyLibraryCatalog legacy) { this.legacy = legacy; }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        String raw = legacy.lookupRecord(isbn);
        if (raw == null) return Optional.empty();
        String[] parts = raw.split("\\|");
        return Optional.of(new Book(parts[0], parts[1], convertGenre(parts[2])));
    }

    private Genre convertGenre(String code) {
        switch (code) {
            case "FNT": return Genre.FANTASY;
            case "SCF": return Genre.SCI_FI;
            case "DET": return Genre.DETECTIVE;
            case "ROM": return Genre.ROMANCE;
            case "NFC": return Genre.NON_FICTION;
            case "POE": return Genre.POETRY;
            case "CLS":
            default:    return Genre.CLASSIC;
        }
    }
}

// ============================ 9. FACADE =============================

class Match {
    final User user;
    final double score;
    Match(User user, double score) { this.user = user; this.score = score; }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s — %.0f%% compatible", user, score * 100);
    }
}

class BookLoversClub {
    public static final String EVENT_NEW_MATCH = "NEW_MATCH";
    public static final String EVENT_MEETUP    = "CLUB_MEETUP";

    private final UserRepository repository = UserRepository.getInstance();
    private final ClubEventBus eventBus = new ClubEventBus();
    private final BookCatalog catalog = new LegacyCatalogAdapter(new LegacyLibraryCatalog());
    private final RegistrationValidator validator;
    private final Map<String, UserSubscriber> subscribers = new HashMap<>();

    BookLoversClub() {
        validator = new NameValidator();
        validator.linkWith(new AgeValidator())
                 .linkWith(new GenresValidator())
                 .linkWith(new ContactValidator());
    }

    /** Registration: validation + saving + subscribing to events. */
    boolean register(User user) {
        String error = validator.validate(user);
        if (error != null) {
            System.out.println("❌ Registration of " + user.getName() + " rejected: " + error);
            return false;
        }
        repository.save(user);
        UserSubscriber sub = new UserSubscriber(user);
        subscribers.put(user.getId(), sub);
        eventBus.subscribe(EVENT_MEETUP, sub);
        System.out.println("✔ Registered: " + user);
        return true;
    }

    void verify(User user) { user.setVerified(true); }

    /** Adds a book the user has read, looked up by ISBN through the legacy catalog adapter. */
    void addReadBook(User user, String isbn) {
        Optional<Book> book = catalog.findByIsbn(isbn);
        if (book.isPresent()) {
            user.addBook(book.get());
            System.out.println("   " + user.getName() + " has read " + book.get());
        } else {
            System.out.println("   Book with ISBN " + isbn + " was not found in the catalog");
        }
    }

    /** Finds the best matches using the chosen strategy. */
    List<Match> findMatches(User user, MatchStrategy strategy, int limit) {
        return repository.findAll().stream()
                .filter(other -> !other.getId().equals(user.getId()))
                .map(other -> new Match(other, strategy.score(user, other)))
                .filter(m -> m.score > 0)
                .sorted((m1, m2) -> Double.compare(m2.score, m1.score))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** Introduction: both users get a notification. */
    void introduce(User a, User b, MatchStrategy strategy) {
        double score = strategy.score(a, b);
        String pct = String.format(Locale.US, "%.0f%%", score * 100);
        Set<Genre> common = commonGenres(a, b);
        subscribers.get(a.getId()).onEvent(EVENT_NEW_MATCH,
                "Meet " + b.getName() + " (" + pct + " match). Shared genres: " + common);
        subscribers.get(b.getId()).onEvent(EVENT_NEW_MATCH,
                "Meet " + a.getName() + " (" + pct + " match). Shared genres: " + common);
    }

    /** Book club meetup — broadcast to all subscribers. */
    void announceMeetup(String bookTitle, String place, String date) {
        eventBus.publish(EVENT_MEETUP, "Discussing \"" + bookTitle + "\" — " + place + ", " + date);
    }

    /** Renders a profile wrapped in decorators. */
    String showProfile(User user) {
        ProfileView view = new BasicProfileView(user);
        view = new ReadingStatsDecorator(view, user);
        view = new LastBookDecorator(view, user);
        if (user.isVerified()) view = new VerifiedBadgeDecorator(view);
        return view.render();
    }

    int membersCount() { return repository.count(); }

    /** Added in MD1: add a new contact channel to the club. */
    void addContactChannel(ContactChannel channel) {
        NotifierFactory.register(channel);
        System.out.println("✔ New contact channel added: " + channel.code());
    }

    Set<String> contactChannels() { return NotifierFactory.registeredCodes(); }

    private Set<Genre> commonGenres(User a, User b) {
        Set<Genre> s = new HashSet<>(a.getFavoriteGenres());
        s.retainAll(b.getFavoriteGenres());
        return s;
    }
}

// ================= NEW CHANNEL (MD1 demo) ===================
// For WhatsApp we did not change the code above. We only wrote these
// 2 classes and called club.addContactChannel(...) in main().

class WhatsAppNotifier implements Notifier {
    public void send(User to, String message) {
        System.out.println("   [WhatsApp -> " + to.getContact() + "] " + message);
    }
}

class WhatsAppChannel implements ContactChannel {
    public String code()              { return "WHATSAPP"; }
    public Notifier createNotifier()  { return new WhatsAppNotifier(); }
    public String checkContact(String c) {
        return c.matches("\\+?\\d{10,15}") ? null
                : "Wrong WhatsApp number (use only digits, for example +77071234567)";
    }
}

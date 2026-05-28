import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FlashcardApp - A GUI-based Flashcard Quiz Application built with Java Swing.
 * Users can add, edit, delete, shuffle, and quiz themselves on flashcards.
 * Cards are automatically saved to and loaded from a local text file.
 */
public class FlashcardApp extends JFrame {

    /**
     * Flashcard - Represents a single flashcard with a question and an answer.
     * This is a static inner class so it can be used without creating a FlashcardApp instance.
     */
    static class Flashcard {
        private String question;
        private String answer;

        /**
         * Constructor to create a new Flashcard.
         * @param question The question shown on the front of the card.
         * @param answer   The answer shown on the back of the card.
         */
        public Flashcard(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        // Returns the question text of this flashcard
        public String getQuestion() { return question; }

        // Updates the question text of this flashcard
        public void setQuestion(String question) { this.question = question; }

        // Returns the answer text of this flashcard
        public String getAnswer() { return answer; }

        // Updates the answer text of this flashcard
        public void setAnswer(String answer) { this.answer = answer; }
    }

    // The full list of flashcards currently loaded in the app
    private final List<Flashcard> deck = new ArrayList<>();

    // Name of the file where flashcards are saved and loaded from
    private final String SAVE_FILE = "flashcards.txt";

    // Tracks which card is currently being shown (index into the deck list)
    private int currentIndex = 0;

    // Whether the card is currently displaying the answer (true) or the question (false)
    private boolean showingAnswer = false;

    // Tracks total quiz attempts and correct answers across all quiz sessions
    private int totalAttempts = 0;
    private int totalCorrect = 0;

    // ── UI Component Declarations ────────────────────────────────────────────────

    private JLabel titleLabel;       // App title shown at the top
    private JLabel cardDisplay;      // The main card area that shows question or answer
    private JLabel statusLabel;      // Shows "Card X of Y" below the card
    private JButton showAnswerButton; // Toggles between question and answer
    private JButton previousButton;   // Navigates to the previous card
    private JButton nextButton;       // Navigates to the next card
    private JButton addButton;        // Opens dialog to add a new card
    private JButton editButton;       // Opens dialog to edit the current card
    private JButton deleteButton;     // Deletes the current card after confirmation
    private JButton shuffleButton;    // Randomly shuffles the deck
    private JButton quizButton;       // Starts an interactive quiz session
    private JButton scoreButton;      // Shows the overall score history

    /**
     * Constructor - Entry point for building the full app window.
     * Loads saved cards, sets up the window, creates all components,
     * lays them out, wires up button events, then shows the first card.
     */
    public FlashcardApp() {
        loadFromFile();      // Load existing cards from flashcards.txt
        setupWindow();       // Configure JFrame properties
        createComponents();  // Create all buttons and labels
        addComponents();     // Add components to the window layout
        attachEvents();      // Connect buttons to their action methods
        updateCardView();    // Render the first card on screen
    }

    /**
     * Configures the main JFrame window properties such as title,
     * size, background color, and close behavior.
     */
    private void setupWindow() {
        setTitle("Flashcard Quiz App");
        setSize(750, 520);
        setMinimumSize(new Dimension(650, 450));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close app when window is closed
        setLocationRelativeTo(null);                    // Center the window on screen
        setLayout(new BorderLayout(12, 12));
        getContentPane().setBackground(new Color(245, 247, 250)); // Light grey-blue background
    }

    /**
     * Creates and styles all UI components (labels and buttons).
     * Does not add them to the window yet — that happens in addComponents().
     */
    private void createComponents() {
        // App title label at the top of the window
        titleLabel = new JLabel("Flashcard Quiz App", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));

        // The card display box — shows either the question or the answer
        cardDisplay = new JLabel("", SwingConstants.CENTER);
        cardDisplay.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        cardDisplay.setOpaque(true);                              // Needed so background color shows
        cardDisplay.setBackground(Color.WHITE);
        cardDisplay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 200, 210), 2, true), // Outer border
                BorderFactory.createEmptyBorder(25, 25, 25, 25)                    // Inner padding
        ));

        // Status label below the card showing current card position
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Create all action buttons
        showAnswerButton = new JButton("Show Answer");
        previousButton   = new JButton("Previous");
        nextButton       = new JButton("Next");
        addButton        = new JButton("Add Card");
        editButton       = new JButton("Edit Card");
        deleteButton     = new JButton("Delete Card");
        shuffleButton    = new JButton("Shuffle");
        quizButton       = new JButton("Quiz Mode");
        scoreButton      = new JButton("Score Board");

        // Apply consistent font and cursor styling to all buttons
        JButton[] buttons = {showAnswerButton, previousButton, nextButton, addButton,
                             editButton, deleteButton, shuffleButton, quizButton, scoreButton};
        for (JButton button : buttons) {
            button.setFont(new Font("Segoe UI", Font.BOLD, 14));
            button.setFocusPainted(false);                        // Remove focus outline on click
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));     // Show hand cursor on hover
        }
    }

    /**
     * Organizes and adds all components to the window using panel layouts.
     * Layout structure:
     *   NORTH  → title label
     *   CENTER → card display + status label
     *   SOUTH  → navigation buttons (row 1) + management buttons (row 2)
     */
    private void addComponents() {
        // Top panel holds the app title
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.CENTER);

        // Center panel holds the flashcard display and the status label
        JPanel cardPanel = new JPanel(new BorderLayout(10, 10));
        cardPanel.setOpaque(false);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30)); // Side margins
        cardPanel.add(cardDisplay, BorderLayout.CENTER);
        cardPanel.add(statusLabel, BorderLayout.SOUTH);

        // Navigation row: Previous | Show Answer | Next
        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        navigationPanel.setOpaque(false);
        navigationPanel.add(previousButton);
        navigationPanel.add(showAnswerButton);
        navigationPanel.add(nextButton);

        // Management row: Add | Edit | Delete | Shuffle | Quiz | Score
        JPanel managePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        managePanel.setOpaque(false);
        managePanel.add(addButton);
        managePanel.add(editButton);
        managePanel.add(deleteButton);
        managePanel.add(shuffleButton);
        managePanel.add(quizButton);
        managePanel.add(scoreButton);

        // Stack navigation and management panels vertically at the bottom
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
        bottomPanel.setOpaque(false);
        bottomPanel.add(navigationPanel);
        bottomPanel.add(managePanel);

        // Add all panels to the main window
        add(topPanel,    BorderLayout.NORTH);
        add(cardPanel,   BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Wires each button to its corresponding action method using lambda listeners.
     */
    private void attachEvents() {
        previousButton.addActionListener(e -> moveCard(-1));     // Go back one card
        nextButton.addActionListener(e -> moveCard(1));          // Go forward one card
        showAnswerButton.addActionListener(e -> toggleAnswer()); // Flip the card
        addButton.addActionListener(e -> addCard());             // Open add dialog
        editButton.addActionListener(e -> editCard());           // Open edit dialog
        deleteButton.addActionListener(e -> deleteCard());       // Confirm and delete card
        shuffleButton.addActionListener(e -> shuffleCards());    // Shuffle the deck
        quizButton.addActionListener(e -> startQuizMode());      // Begin quiz session
        scoreButton.addActionListener(e -> showScoreBoard());    // Show score history
    }

    /**
     * Refreshes the card display area based on the current state.
     * Disables buttons when the deck is empty.
     * Shows question or answer depending on showingAnswer flag.
     */
    private void updateCardView() {
        boolean hasCards = !deck.isEmpty();

        // Disable interactive buttons when there are no cards
        previousButton.setEnabled(hasCards);
        nextButton.setEnabled(hasCards);
        showAnswerButton.setEnabled(hasCards);
        editButton.setEnabled(hasCards);
        deleteButton.setEnabled(hasCards);
        shuffleButton.setEnabled(hasCards);
        quizButton.setEnabled(hasCards);

        // Show a placeholder message if the deck is empty
        if (!hasCards) {
            cardDisplay.setText("<html><center>No flashcards yet.<br>Click Add Card to create your first card.</center></html>");
            statusLabel.setText("0 cards available");
            return;
        }

        // Get the current card and decide whether to show question or answer
        Flashcard current = deck.get(currentIndex);
        String label = showingAnswer ? "ANSWER" : "QUESTION";
        String text  = showingAnswer ? current.getAnswer() : current.getQuestion();

        // Render the card content using HTML for multi-line support
        cardDisplay.setText("<html><center><b>" + escapeHtml(label) + ":</b><br><br>" + escapeHtml(text) + "</center></html>");

        // Change background color to indicate answer is showing (light blue vs white)
        cardDisplay.setBackground(showingAnswer ? new Color(230, 242, 255) : Color.WHITE);

        // Update the button label to match the current state
        showAnswerButton.setText(showingAnswer ? "Show Question" : "Show Answer");

        // Update position indicator below the card
        statusLabel.setText("Card " + (currentIndex + 1) + " of " + deck.size());
    }

    /**
     * Flips the current card between showing the question and the answer.
     */
    private void toggleAnswer() {
        if (deck.isEmpty()) return;
        showingAnswer = !showingAnswer; // Toggle the flag
        updateCardView();
    }

    /**
     * Moves to a different card in the deck.
     * Wraps around at both ends (last → first, first → last).
     * @param step +1 to go forward, -1 to go backward.
     */
    private void moveCard(int step) {
        if (deck.isEmpty()) return;
        currentIndex += step;
        // Wrap around if we go past the last card
        if (currentIndex >= deck.size()) currentIndex = 0;
        // Wrap around if we go before the first card
        if (currentIndex < 0) currentIndex = deck.size() - 1;
        showingAnswer = false; // Always show question side when navigating
        updateCardView();
    }

    /**
     * Opens a dialog for the user to enter a new flashcard's question and answer.
     * Saves the new card to file after adding it.
     */
    private void addCard() {
        JTextField questionField = new JTextField();
        JTextField answerField   = new JTextField();
        Object[] fields = {"Question:", questionField, "Answer:", answerField};

        int option = JOptionPane.showConfirmDialog(this, fields, "Add New Flashcard", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String question = questionField.getText().trim();
            String answer   = answerField.getText().trim();

            // Validate that neither field is empty
            if (question.isEmpty() || answer.isEmpty()) {
                showMessage("Question and answer cannot be empty.");
                return;
            }

            deck.add(new Flashcard(question, answer));
            currentIndex  = deck.size() - 1; // Jump to the newly added card
            showingAnswer = false;
            saveToFile();
            updateCardView();
        }
    }

    /**
     * Opens a dialog pre-filled with the current card's data so the user can edit it.
     * Saves the updated card to file after editing.
     */
    private void editCard() {
        if (deck.isEmpty()) return;
        Flashcard current = deck.get(currentIndex);

        // Pre-fill fields with the current card's content
        JTextField questionField = new JTextField(current.getQuestion());
        JTextField answerField   = new JTextField(current.getAnswer());
        Object[] fields = {"Question:", questionField, "Answer:", answerField};

        int option = JOptionPane.showConfirmDialog(this, fields, "Edit Flashcard", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String question = questionField.getText().trim();
            String answer   = answerField.getText().trim();

            // Validate that neither field is left empty
            if (question.isEmpty() || answer.isEmpty()) {
                showMessage("Question and answer cannot be empty.");
                return;
            }

            current.setQuestion(question);
            current.setAnswer(answer);
            showingAnswer = false;
            saveToFile();
            updateCardView();
        }
    }

    /**
     * Asks the user to confirm, then removes the current flashcard from the deck.
     * Adjusts the currentIndex to stay within bounds after deletion.
     */
    private void deleteCard() {
        if (deck.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Delete this flashcard?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            deck.remove(currentIndex);

            // If currentIndex is now out of bounds, move it back to the last card
            if (currentIndex >= deck.size() && !deck.isEmpty()) currentIndex = deck.size() - 1;
            if (deck.isEmpty()) currentIndex = 0; // Reset if deck is now empty

            showingAnswer = false;
            saveToFile();
            updateCardView();
        }
    }

    /**
     * Randomly shuffles the order of all flashcards in the deck.
     * Resets to the first card after shuffling and saves the new order.
     */
    private void shuffleCards() {
        if (deck.isEmpty()) return;
        Collections.shuffle(deck); // Java built-in random shuffle
        currentIndex  = 0;
        showingAnswer = false;
        saveToFile();
        updateCardView();
        showMessage("Cards shuffled successfully.");
    }

    /**
     * Starts an interactive quiz session.
     * Shuffles a copy of the deck, prompts the user for each answer,
     * checks if it matches (case-insensitive), and shows a result summary.
     * Updates the global score totals at the end.
     */
    private void startQuizMode() {
        if (deck.isEmpty()) return;

        // Work on a shuffled copy so the original deck order isn't changed
        List<Flashcard> quizCards = new ArrayList<>(deck);
        Collections.shuffle(quizCards);

        int sessionAttempts = 0;
        int sessionCorrect  = 0;

        for (Flashcard card : quizCards) {
            // Prompt user to answer the current question
            String answer = JOptionPane.showInputDialog(this,
                    "Question:\n" + card.getQuestion() + "\n\nType your answer:",
                    "Quiz Mode",
                    JOptionPane.QUESTION_MESSAGE);

            if (answer == null) break;      // User cancelled the quiz
            answer = answer.trim();
            if (answer.isEmpty()) continue; // Skip if user typed nothing

            sessionAttempts++;
            totalAttempts++;

            // Compare answer ignoring case differences
            if (answer.equalsIgnoreCase(card.getAnswer().trim())) {
                sessionCorrect++;
                totalCorrect++;
                showMessage("Correct!");
            } else {
                showMessage("Wrong. Correct answer: " + card.getAnswer());
            }
        }

        // Calculate and display this session's result
        int wrong   = sessionAttempts - sessionCorrect;
        int percent = sessionAttempts == 0 ? 0 : (sessionCorrect * 100) / sessionAttempts;
        showMessage("Quiz Result\nAnswered: " + sessionAttempts +
                    "\nCorrect: "  + sessionCorrect +
                    "\nWrong: "    + wrong +
                    "\nScore: "    + percent + "%");
    }

    /**
     * Displays a summary of the user's overall quiz performance
     * across all sessions since the app was launched.
     */
    private void showScoreBoard() {
        int wrong   = totalAttempts - totalCorrect;
        int percent = totalAttempts == 0 ? 0 : (totalCorrect * 100) / totalAttempts;
        showMessage("Score Board\nTotal Attempts: " + totalAttempts +
                    "\nTotal Correct: "  + totalCorrect +
                    "\nTotal Wrong: "    + wrong +
                    "\nOverall Score: "  + percent + "%");
    }

    /**
     * Saves all current flashcards to a text file (flashcards.txt).
     * Each card is stored on one line, with question and answer separated by "||||".
     * Shows an error dialog if the file cannot be written.
     */
    private void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SAVE_FILE))) {
            for (Flashcard card : deck) {
                // Replace any accidental "||||" in user text to avoid corrupting the format
                writer.println(card.getQuestion().replace("||||", " ") + "||||" +
                               card.getAnswer().replace("||||", " "));
            }
        } catch (IOException e) {
            showMessage("Could not save flashcards: " + e.getMessage());
        }
    }

    /**
     * Loads flashcards from the save file when the app starts.
     * If no save file exists, three default sample cards are added.
     * Each line is split on "||||" to separate question from answer.
     */
    private void loadFromFile() {
        File file = new File(SAVE_FILE);

        // If no file exists yet, populate with starter cards
        if (!file.exists()) {
            deck.add(new Flashcard("What is Java?", "A high-level object-oriented programming language."));
            deck.add(new Flashcard("What is OOP?", "Object-Oriented Programming."));
            deck.add(new Flashcard("What does SQL stand for?", "Structured Query Language."));
            saveToFileSilently(); // Save defaults without showing error dialogs
            return;
        }

        // Read each line and split into question and answer
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|\\|\\|\\|", 2); // Split on "||||", max 2 parts
                if (parts.length == 2) {
                    deck.add(new Flashcard(parts[0].trim(), parts[1].trim()));
                }
            }
        } catch (IOException e) {
            System.out.println("Could not load flashcards: " + e.getMessage());
        }
    }

    /**
     * Same as saveToFile() but silently ignores any IO errors.
     * Used during initialization when the UI is not fully set up yet
     * (so we can't show dialog boxes yet).
     */
    private void saveToFileSilently() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SAVE_FILE))) {
            for (Flashcard card : deck) {
                writer.println(card.getQuestion() + "||||" + card.getAnswer());
            }
        } catch (IOException ignored) {
            // Silently ignored — not critical during startup
        }
    }

    /**
     * Escapes special HTML characters in a string so it renders correctly
     * inside a JLabel that uses HTML formatting.
     * Converts: & → &amp;  < → &lt;  > → &gt;  newline → <br>
     * @param text The raw text to escape.
     * @return The HTML-safe version of the text.
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\n", "<br>");
    }

    /**
     * Displays a simple information dialog with the given message.
     * @param message The text to show inside the dialog.
     */
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    /**
     * Main method — entry point of the application.
     * Uses SwingUtilities.invokeLater to safely launch the GUI
     * on the Event Dispatch Thread (required by Swing).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FlashcardApp().setVisible(true));
    }
}

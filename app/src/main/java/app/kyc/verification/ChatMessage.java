package app.kyc.verification;

public class ChatMessage {
    private String text;
    private boolean isBot;
    private long timestamp;

    public ChatMessage(String text, boolean isBot) {
        this.text = text;
        this.isBot = isBot;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() { return text; }
    public boolean isBot() { return isBot; }
    public long getTimestamp() { return timestamp; }

    public String getFormattedTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
}

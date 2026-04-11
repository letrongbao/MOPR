package com.example.myapplication.features.home;

/**
 * Model đại diện cho một thông báo hiển thị trên màn hình Thông báo.
 * Bao gồm cả thông báo hệ thống (tạo từ dữ liệu Contract) và thông báo
 * thông thường (lấy từ collection "notifications" trên Firestore).
 */
public class NotificationItem {

    // ---- Loại thông báo ----
    public static final int TYPE_SYSTEM  = 0; // Thông báo hệ thống (màu cam nổi bật)
    public static final int TYPE_REGULAR = 1; // Thông báo thường (màu xanh/xám)

    // ---- Icon ký hiệu ----
    public static final String ICON_CONTRACT = "📄";
    public static final String ICON_PAYMENT  = "💰";
    public static final String ICON_DEPOSIT  = "🏦";
    public static final String ICON_INFO     = "ℹ️";

    private final int    type;      // TYPE_SYSTEM hoặc TYPE_REGULAR
    private final String icon;      // Emoji icon hiển thị bên trái
    private final String title;     // Tiêu đề ngắn gọn
    private final String body;      // Nội dung chi tiết
    private final String timeLabel; // Nhãn thời gian (ví dụ: "Hôm nay", "2 ngày trước")

    public NotificationItem(int type, String icon, String title, String body, String timeLabel) {
        this.type      = type;
        this.icon      = icon;
        this.title     = title;
        this.body      = body;
        this.timeLabel = timeLabel;
    }

    // ---- Getters ----
    public int    getType()      { return type;      }
    public String getIcon()      { return icon;      }
    public String getTitle()     { return title;     }
    public String getBody()      { return body;      }
    public String getTimeLabel() { return timeLabel; }

    public boolean isSystemNotification() { return type == TYPE_SYSTEM; }
}

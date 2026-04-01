package com.example.myapplication.core.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.myapplication.R;
import com.example.myapplication.core.constants.InvoiceStatus;
import com.example.myapplication.features.auth.MainActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class HoaDonWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_hoa_don);

        // Click widget -> mở app
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.tvWidgetHoaDon, pendingIntent);

        // Hiển thị thời gian cập nhật
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        views.setTextViewText(R.id.tvWidgetUpdate, "Cập nhật: " + time);

        // Lấy dữ liệu từ Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            views.setTextViewText(R.id.tvWidgetHoaDon, "Chưa đăng nhập");
            views.setTextViewText(R.id.tvWidgetPhong, "Mở app để đăng nhập");
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        // Hiển thị "Đang tải..." trước
        views.setTextViewText(R.id.tvWidgetHoaDon, "Đang tải...");
        appWidgetManager.updateAppWidget(appWidgetId, views);

        String uid = user.getUid();
        String tenantId = context.getSharedPreferences("NhaTroPrefs", Context.MODE_PRIVATE)
                .getString("activeTenantId", null);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Dùng background thread với Tasks.await() thay vì async callback
        // để đảm bảo widget được update trước khi process bị kill
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Đếm hóa đơn chưa thanh toán (blocking call)
                Task<QuerySnapshot> hoaDonTask = (tenantId != null && !tenantId.isEmpty())
                        ? db.collection("tenants").document(tenantId).collection("hoa_don").get()
                        : db.collection("users").document(uid).collection("hoa_don").get();
                QuerySnapshot hoaDonSnap = Tasks.await(hoaDonTask);

                int chuaThanhToan = 0;
                for (QueryDocumentSnapshot doc : hoaDonSnap) {
                    String trangThai = doc.getString("trangThai");
                    if (InvoiceStatus.REPORTED.equals(trangThai) || InvoiceStatus.PARTIAL.equals(trangThai)) {
                        chuaThanhToan++;
                    }
                }
                views.setTextViewText(R.id.tvWidgetHoaDon,
                        "Hóa đơn chưa TT: " + chuaThanhToan);

                // Đếm phòng (blocking call)
                Task<QuerySnapshot> phongTask = (tenantId != null && !tenantId.isEmpty())
                        ? db.collection("tenants").document(tenantId).collection("phong_tro").get()
                        : db.collection("users").document(uid).collection("phong_tro").get();
                QuerySnapshot phongSnap = Tasks.await(phongTask);

                views.setTextViewText(R.id.tvWidgetPhong,
                        "Tổng phòng: " + phongSnap.size());
            } catch (Exception e) {
                views.setTextViewText(R.id.tvWidgetHoaDon, "Lỗi kết nối");
                views.setTextViewText(R.id.tvWidgetPhong, "Lỗi tải dữ liệu");
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        });
    }
}

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

public class InvoiceWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_invoice);

        // Internal note.
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.tvWidgetInvoice, pendingIntent);

        // Internal note.
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        views.setTextViewText(R.id.tvWidgetUpdate, context.getString(R.string.updated_colon) + time);

        // Internal note.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            views.setTextViewText(R.id.tvWidgetInvoice, context.getString(R.string.not_logged_in));
            views.setTextViewText(R.id.tvWidgetPhong, context.getString(R.string.open_app_to_login));
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        // Internal note.
        views.setTextViewText(R.id.tvWidgetInvoice, context.getString(R.string.loading));
        appWidgetManager.updateAppWidget(appWidgetId, views);

        String uid = user.getUid();
        String tenantId = context.getSharedPreferences("NhaTroPrefs", Context.MODE_PRIVATE)
                .getString("activeTenantId", null);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Internal note.
        // Internal note.
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Internal note.
                Task<QuerySnapshot> invoiceTask = (tenantId != null && !tenantId.isEmpty())
                        ? db.collection("tenants").document(tenantId).collection("invoices").get()
                        : db.collection("users").document(uid).collection("invoices").get();
                QuerySnapshot invoiceSnapshot = Tasks.await(invoiceTask);

                int unpaidCount = 0;
                for (QueryDocumentSnapshot doc : invoiceSnapshot) {
                    String status = doc.getString("status");
                    if (InvoiceStatus.REPORTED.equals(status) || InvoiceStatus.PARTIAL.equals(status)) {
                        unpaidCount++;
                    }
                }
                views.setTextViewText(R.id.tvWidgetInvoice,
                        context.getString(R.string.widget_unpaid_invoices, unpaidCount));

                // Internal note.
                Task<QuerySnapshot> roomTask = (tenantId != null && !tenantId.isEmpty())
                        ? db.collection("tenants").document(tenantId).collection("rooms").get()
                        : db.collection("users").document(uid).collection("rooms").get();
                QuerySnapshot roomSnapshot = Tasks.await(roomTask);

                views.setTextViewText(R.id.tvWidgetPhong,
                        context.getString(R.string.widget_total_rooms, roomSnapshot.size()));
            } catch (Exception e) {
                views.setTextViewText(R.id.tvWidgetInvoice, context.getString(R.string.connection_error));
                views.setTextViewText(R.id.tvWidgetPhong, context.getString(R.string.error_load_widget_data));
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        });
    }
}

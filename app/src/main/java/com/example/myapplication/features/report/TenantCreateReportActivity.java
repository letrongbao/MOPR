package com.example.myapplication.features.report;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.core.session.TenantSession;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TenantCreateReportActivity extends AppCompatActivity {

    // ---- Views ----
    private LinearLayout  layoutDatePicker;
    private TextView      tvSelectedDate;
    private EditText      etTitle;
    private EditText      etDescription;
    private LinearLayout  btnCamera;
    private LinearLayout  btnGallery;
    private LinearLayout  layoutUploadPlaceholder;
    private HorizontalScrollView scrollThumbnails;
    private LinearLayout  llThumbnails;
    private Button        btnClose;
    private Button        btnSubmit;

    // ---- State ----
    private Calendar      selectedCalendar = null;
    private final List<Uri> selectedImages = new ArrayList<>();
    private static final int MAX_IMAGES = 4;

    private static final SimpleDateFormat DISPLAY_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    // ---- Data ----
    private String roomId;
    private String tenantId;

    private FirebaseFirestore db;
    private FirebaseAuth      mAuth;

    // ---- Gallery picker ----
    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    int remaining = MAX_IMAGES - selectedImages.size();
                    if (remaining <= 0) {
                        Toast.makeText(this, "Đã đủ " + MAX_IMAGES + " hình ảnh!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int toAdd = Math.min(remaining, uris.size());
                    for (int i = 0; i < toAdd; i++) {
                        selectedImages.add(uris.get(i));
                    }
                    if (uris.size() > remaining) {
                        Toast.makeText(this, "Chỉ thêm được " + remaining + " ảnh nữa, đã đủ " + MAX_IMAGES + "!", Toast.LENGTH_SHORT).show();
                    }
                    updateImageThumbnails();
                }
            });

    // ---- Camera picker (single image) ----
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri photoUri = result.getData().getData();
                    if (photoUri != null) {
                        if (selectedImages.size() >= MAX_IMAGES) {
                            Toast.makeText(this, "Đã đủ " + MAX_IMAGES + " hình ảnh!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectedImages.add(photoUri);
                        updateImageThumbnails();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_create_report);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        roomId   = getIntent().getStringExtra(TenantReportListActivity.EXTRA_ROOM_ID);
        tenantId = getIntent().getStringExtra(TenantReportListActivity.EXTRA_TENANT_ID);

        // Fallback tenantId từ Session nếu Intent không truyền
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = TenantSession.getActiveTenantId();
        }

        bindViews();
        setupListeners();
    }

    // ================================================================
    //  Ánh xạ View
    // ================================================================
    private void bindViews() {
        layoutDatePicker        = findViewById(R.id.layoutDatePicker);
        tvSelectedDate          = findViewById(R.id.tvSelectedDate);
        etTitle                 = findViewById(R.id.etTitle);
        etDescription           = findViewById(R.id.etDescription);
        btnCamera               = findViewById(R.id.btnCamera);
        btnGallery              = findViewById(R.id.btnGallery);
        layoutUploadPlaceholder = findViewById(R.id.layoutUploadPlaceholder);
        scrollThumbnails        = findViewById(R.id.scrollThumbnails);
        llThumbnails            = findViewById(R.id.llThumbnails);
        btnClose                = findViewById(R.id.btnClose);
        btnSubmit               = findViewById(R.id.btnSubmit);
    }

    // ================================================================
    //  Setup listeners
    // ================================================================
    private void setupListeners() {
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Đóng
        btnClose.setOnClickListener(v -> finish());

        // Chọn ngày hẹn
        layoutDatePicker.setOnClickListener(v -> showDateTimePicker());

        // Chụp ảnh (mở gallery vì camera cần FileProvider phức tạp hơn)
        btnCamera.setOnClickListener(v -> openGallery());

        // Thêm từ thư viện
        btnGallery.setOnClickListener(v -> openGallery());

        // Nút Thêm phản ánh
        btnSubmit.setOnClickListener(v -> submitReport());
    }

    // ================================================================
    //  MaterialDatePicker + MaterialTimePicker
    // ================================================================
    private void showDateTimePicker() {
        // Mặc định chọn ngày mai nếu chưa có ngày trước đó
        long initialSelection = selectedCalendar != null
                ? selectedCalendar.getTimeInMillis()
                : System.currentTimeMillis();

        // Chỉ cho phép chọn từ hôm nay trở về sau
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder
                .datePicker()
                .setTitleText("Chọn ngày hẹn sửa chữa")
                .setSelection(initialSelection)
                .setCalendarConstraints(constraints)
                .build();

        datePicker.addOnPositiveButtonClickListener(dateMillis -> {
            // Sau khi chọn ngày, mở TimePicker
            Calendar tempCal = Calendar.getInstance();
            tempCal.setTimeInMillis(dateMillis);
            int pickedYear  = tempCal.get(Calendar.YEAR);
            int pickedMonth = tempCal.get(Calendar.MONTH);
            int pickedDay   = tempCal.get(Calendar.DAY_OF_MONTH);

            int initHour   = selectedCalendar != null ? selectedCalendar.get(Calendar.HOUR_OF_DAY) : Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int initMinute = selectedCalendar != null ? selectedCalendar.get(Calendar.MINUTE)       : 0;

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(initHour)
                    .setMinute(initMinute)
                    .setTitleText("Chọn giờ hẹn")
                    .build();

            timePicker.addOnPositiveButtonClickListener(v -> {
                selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(pickedYear, pickedMonth, pickedDay,
                        timePicker.getHour(), timePicker.getMinute(), 0);
                selectedCalendar.set(Calendar.MILLISECOND, 0);
                tvSelectedDate.setText(DISPLAY_FORMAT.format(selectedCalendar.getTime()));
                tvSelectedDate.setTextColor(getResources().getColor(android.R.color.black));
            });

            timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    // ================================================================
    //  Gallery picker
    // ================================================================
    private void openGallery() {
        if (selectedImages.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Tối đa " + MAX_IMAGES + " hình ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }
        galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    // ================================================================
    //  Cập nhật thumbnail
    // ================================================================
    private void updateImageThumbnails() {
        llThumbnails.removeAllViews();

        if (selectedImages.isEmpty()) {
            scrollThumbnails.setVisibility(View.GONE);
            layoutUploadPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        scrollThumbnails.setVisibility(View.VISIBLE);
        layoutUploadPlaceholder.setVisibility(View.GONE);

        int sizePx = (int) (80 * getResources().getDisplayMetrics().density);
        int marginPx = (int) (8 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < selectedImages.size(); i++) {
            final int idx = i;
            Uri uri = selectedImages.get(i);

            // FrameLayout chứa ảnh + nút xóa
            android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(sizePx, sizePx);
            frameParams.setMarginEnd(marginPx);
            frame.setLayoutParams(frameParams);

            // ImageView thumbnail
            ImageView imgThumb = new ImageView(this);
            android.widget.FrameLayout.LayoutParams imgParams =
                    new android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
            imgThumb.setLayoutParams(imgParams);
            imgThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // Bo tròn góc ảnh
            imgThumb.setClipToOutline(true);
            android.graphics.drawable.GradientDrawable roundBg = new android.graphics.drawable.GradientDrawable();
            roundBg.setCornerRadius(8 * getResources().getDisplayMetrics().density);
            roundBg.setColor(0xFFEEEEEE);
            imgThumb.setBackground(roundBg);

            Glide.with(this).load(uri).centerCrop().into(imgThumb);

            // Nút xóa (X) ở góc trên phải
            TextView btnRemove = new TextView(this);
            int removeSizePx = (int) (20 * getResources().getDisplayMetrics().density);
            android.widget.FrameLayout.LayoutParams removeParams =
                    new android.widget.FrameLayout.LayoutParams(removeSizePx, removeSizePx);
            removeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            btnRemove.setLayoutParams(removeParams);
            btnRemove.setText("✕");
            btnRemove.setTextSize(10f);
            btnRemove.setGravity(android.view.Gravity.CENTER);
            btnRemove.setTextColor(0xFFFFFFFF);
            btnRemove.setBackground(createCircleBg(0xAAFF0000));
            btnRemove.setOnClickListener(v -> {
                selectedImages.remove(idx);
                updateImageThumbnails();
            });

            frame.addView(imgThumb);
            frame.addView(btnRemove);
            llThumbnails.addView(frame);
        }

        // Ô thêm ảnh nếu chưa đủ 4
        if (selectedImages.size() < MAX_IMAGES) {
            LinearLayout addMore = new LinearLayout(this);
            LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(sizePx, sizePx);
            addMore.setLayoutParams(addParams);
            addMore.setGravity(android.view.Gravity.CENTER);
            addMore.setBackground(createRoundedBorder());
            addMore.setClickable(true);
            addMore.setFocusable(true);
            addMore.setOnClickListener(v -> openGallery());

            TextView plusSign = new TextView(this);
            plusSign.setText("+");
            plusSign.setTextSize(24f);
            plusSign.setTextColor(0xFF27AE60);
            plusSign.setGravity(android.view.Gravity.CENTER);
            addMore.addView(plusSign);
            llThumbnails.addView(addMore);
        }
    }

    private android.graphics.drawable.GradientDrawable createCircleBg(int color) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private android.graphics.drawable.GradientDrawable createRoundedBorder() {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        d.setColor(0xFFF5F5F5);
        d.setStroke(2, 0xFF27AE60);
        d.setCornerRadius(8 * getResources().getDisplayMetrics().density);
        return d;
    }

    // ================================================================
    //  Validate và Submit lên Firestore
    // ================================================================
    private void submitReport() {
        String title       = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Validate
        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Vui lòng nhập tên vấn đề");
            etTitle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Vui lòng nhập mô tả vấn đề");
            etDescription.requestFocus();
            return;
        }

        if (tenantId == null || tenantId.isEmpty()) {
            Toast.makeText(this, "Không xác định được tài khoản!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chuẩn bị dữ liệu
        Timestamp now             = Timestamp.now();
        Timestamp appointmentTime = selectedCalendar != null
                ? new Timestamp(selectedCalendar.getTime())
                : null;

        FirebaseUser user     = mAuth.getCurrentUser();
        String       uid      = (user != null) ? user.getUid()   : "";
        String       userName = (user != null && user.getDisplayName() != null)
                                ? user.getDisplayName() : "Khách thuê";

        Map<String, Object> data = new HashMap<>();
        // Thông tin cốt lõi
        data.put("title",           title);
        data.put("description",     description);
        data.put("status",          "PENDING");        // Luôn mặc định Chưa làm
        data.put("priority",        "Trung bình");     // Độ ưu tiên mặc định
        data.put("createdAt",       now);
        data.put("appointmentTime", appointmentTime);
        data.put("images",          Collections.emptyList());
        // Người tạo (khách thuê)
        data.put("tenantId",        tenantId != null ? tenantId : "");
        data.put("createdBy",       uid);
        data.put("createdByName",   userName);
        // Phòng
        data.put("roomId",          roomId != null ? roomId : "");
        // Thông tin chủ nhà (hardcode theo yêu cầu)
        data.put("ownerId",         "iiJcZrGorjRW5adzNMW79BlUKtj2");
        data.put("ownerName",       "Huy");

        // Hiện loading
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");

        // Lưu vào collection gốc "issues" thay vì sub-collection của tenant
        db.collection("issues")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Đã gửi phản ánh thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Báo cho ReportListActivity biết để refresh
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Thêm phản ánh");
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}

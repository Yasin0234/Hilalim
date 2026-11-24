package com.korkutsoftware.hilalim.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.korkutsoftware.hilalim.R;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Generates shareable high-resolution Islamic quote/verse/dua PNG cards
 * and shares them via FileProvider or shares plain text.
 */
public class ShareCardHelper {

    public static void showShareOptionsDialog(Context context, String badgeTitle,
                                              String arabicText, String bodyText, String sourceText) {
        if (context == null) return;
        String[] options = {
                context.getString(R.string.share_as_card),
                context.getString(R.string.share_as_text)
        };
        new AlertDialog.Builder(context)
                .setTitle(R.string.share_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        shareImageCard(context, badgeTitle, arabicText, bodyText, sourceText);
                    } else {
                        StringBuilder sb = new StringBuilder();
                        if (!TextUtils.isEmpty(badgeTitle)) sb.append(badgeTitle).append("\n\n");
                        if (!TextUtils.isEmpty(arabicText)) sb.append(arabicText).append("\n\n");
                        if (!TextUtils.isEmpty(bodyText)) sb.append(bodyText).append("\n");
                        if (!TextUtils.isEmpty(sourceText)) sb.append("\n").append(sourceText);
                        sb.append("\n\n— ").append(context.getString(R.string.app_name));
                        sharePlainText(context, badgeTitle, sb.toString());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public static void sharePlainText(Context context, String subject, String body) {
        if (context == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        if (!TextUtils.isEmpty(subject)) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }
        intent.putExtra(Intent.EXTRA_TEXT, body);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_title)));
    }

    public static void shareImageCard(Context context, String badgeTitle,
                                      String arabicText, String bodyText, String sourceText) {
        if (context == null) return;
        try {
            Bitmap bitmap = renderCardBitmap(context, badgeTitle, arabicText, bodyText, sourceText);
            File dir = new File(context.getCacheDir(), "shared_cards");
            if (!dir.exists() && !dir.mkdirs()) {
                Toast.makeText(context, R.string.share_error, Toast.LENGTH_SHORT).show();
                return;
            }
            // Clean previous temp cards
            File[] existing = dir.listFiles();
            if (existing != null) {
                for (File old : existing) {
                    //noinspection ResultOfMethodCallIgnored
                    old.delete();
                }
            }

            File outFile = new File(dir, "hilalim_card_" + System.currentTimeMillis() + ".png");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos);
            } finally {
                bitmap.recycle();
            }

            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", outFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            if (!TextUtils.isEmpty(bodyText)) {
                intent.putExtra(Intent.EXTRA_TEXT, bodyText + (TextUtils.isEmpty(sourceText) ? "" : " " + sourceText));
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_title)));
        } catch (Exception e) {
            Toast.makeText(context, R.string.share_error, Toast.LENGTH_SHORT).show();
        }
    }

    private static Bitmap renderCardBitmap(Context context, String badgeTitle,
                                           String arabicText, String bodyText, String sourceText) {
        int width = 1080;
        int height = 1350;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Deep emerald-charcoal background gradient
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(
                0, 0, width, height,
                Color.parseColor("#0F2922"),
                Color.parseColor("#111415"),
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, width, height, bgPaint);

        // Subtle gold radial glow at top
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(Color.argb(24, 212, 175, 55));
        canvas.drawCircle(width / 2f, 220f, 380f, glowPaint);

        // Outer and inner decorative gold borders
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.argb(170, 212, 175, 55));
        borderPaint.setStrokeWidth(4f);
        canvas.drawRoundRect(new RectF(48, 48, width - 48, height - 48), 44f, 44f, borderPaint);

        borderPaint.setColor(Color.argb(70, 212, 175, 55));
        borderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(new RectF(68, 68, width - 68, height - 68), 34f, 34f, borderPaint);

        int contentWidth = width - 200;
        float cursorY = 140f;

        // Top badge title
        if (!TextUtils.isEmpty(badgeTitle)) {
            TextPaint badgePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            badgePaint.setColor(Color.parseColor("#D4AF37"));
            badgePaint.setTextSize(34f);
            badgePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            badgePaint.setLetterSpacing(0.08f);
            badgePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(badgeTitle.toUpperCase(), width / 2f, cursorY, badgePaint);
            cursorY += 56f;
        }

        // Gold divider line
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.argb(140, 212, 175, 55));
        linePaint.setStrokeWidth(3f);
        canvas.drawLine(width / 2f - 120f, cursorY, width / 2f + 120f, cursorY, linePaint);
        cursorY += 72f;

        // Measure Arabic + Body + Source to center vertically in available space
        StaticLayout arabicLayout = null;
        if (!TextUtils.isEmpty(arabicText)) {
            TextPaint arPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            arPaint.setColor(Color.parseColor("#F2CA50"));
            arPaint.setTextSize(arabicText.length() > 160 ? 44f : 54f);
            arPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            arabicLayout = StaticLayout.Builder.obtain(arabicText, 0, arabicText.length(), arPaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(14f, 1.0f)
                    .build();
        }

        StaticLayout bodyLayout = null;
        if (!TextUtils.isEmpty(bodyText)) {
            TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            bodyPaint.setColor(Color.parseColor("#F5F3E7"));
            float bodySize = bodyText.length() > 260 ? 34f : (bodyText.length() > 140 ? 40f : 44f);
            bodyPaint.setTextSize(bodySize);
            bodyLayout = StaticLayout.Builder.obtain(bodyText, 0, bodyText.length(), bodyPaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(14f, 1.0f)
                    .build();
        }

        StaticLayout sourceLayout = null;
        if (!TextUtils.isEmpty(sourceText)) {
            TextPaint srcPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            srcPaint.setColor(Color.parseColor("#D4AF37"));
            srcPaint.setTextSize(30f);
            srcPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            sourceLayout = StaticLayout.Builder.obtain(sourceText, 0, sourceText.length(), srcPaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build();
        }

        float totalTextHeight = 0f;
        if (arabicLayout != null) totalTextHeight += arabicLayout.getHeight() + 44f;
        if (bodyLayout != null) totalTextHeight += bodyLayout.getHeight() + 36f;
        if (sourceLayout != null) totalTextHeight += sourceLayout.getHeight();

        float availableBottom = height - 180f;
        float availableSpace = Math.max(0f, availableBottom - cursorY);
        float drawY = cursorY + Math.max(0f, (availableSpace - totalTextHeight) / 2f);

        if (arabicLayout != null) {
            canvas.save();
            canvas.translate(100f, drawY);
            arabicLayout.draw(canvas);
            canvas.restore();
            drawY += arabicLayout.getHeight() + 44f;
        }
        if (bodyLayout != null) {
            canvas.save();
            canvas.translate(100f, drawY);
            bodyLayout.draw(canvas);
            canvas.restore();
            drawY += bodyLayout.getHeight() + 36f;
        }
        if (sourceLayout != null) {
            canvas.save();
            canvas.translate(100f, drawY);
            sourceLayout.draw(canvas);
            canvas.restore();
        }

        // Footer branding
        TextPaint footerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        footerPaint.setColor(Color.argb(190, 212, 175, 55));
        footerPaint.setTextSize(28f);
        footerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        footerPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("☾  " + context.getString(R.string.app_name), width / 2f, height - 102f, footerPaint);

        return bmp;
    }
}

package com.example.beat.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import java.io.File;

public class AlbumArtExtractor {
    private static final String TAG = "AlbumArtExtractor";

    /**
     * Extract album art from a music file
     * @param context Application context
     * @param filePath Path to the music file
     * @return Bitmap of album art or null if not found
     */
    public static Bitmap extractAlbumArt(Context context, String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            // Handle different URI formats
            if (filePath.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(filePath));
            } else if (filePath.startsWith("file://")) {
                String actualPath = filePath.substring(7); // Remove "file://" prefix
                File file = new File(actualPath);
                if (file.exists()) {
                    retriever.setDataSource(actualPath);
                } else {
                    Log.w(TAG, "File does not exist: " + actualPath);
                    return null;
                }
            } else {
                // Assume it's a direct file path
                File file = new File(filePath);
                if (file.exists()) {
                    retriever.setDataSource(filePath);
                } else {
                    Log.w(TAG, "File does not exist: " + filePath);
                    return null;
                }
            }

            // Extract embedded album art
            byte[] albumArtBytes = retriever.getEmbeddedPicture();
            if (albumArtBytes != null) {
                Log.d(TAG, "✅ Successfully extracted embedded album art from: " + filePath);
                
                // Decode with options to avoid memory issues
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // Scale down to reduce memory usage
                options.inPreferredConfig = Bitmap.Config.RGB_565; // Use less memory
                
                return BitmapFactory.decodeByteArray(albumArtBytes, 0, albumArtBytes.length, options);
            } else {
                Log.d(TAG, "❌ No embedded album art found in: " + filePath);
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error extracting album art from: " + filePath + " - " + e.getMessage());
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever: " + e.getMessage());
            }
        }
    }

    /**
     * Check if a file has embedded album art without extracting it
     * @param context Application context
     * @param filePath Path to the music file
     * @return true if album art exists, false otherwise
     */
    public static boolean hasAlbumArt(Context context, String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (filePath.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(filePath));
            } else if (filePath.startsWith("file://")) {
                String actualPath = filePath.substring(7);
                retriever.setDataSource(actualPath);
            } else {
                retriever.setDataSource(filePath);
            }

            byte[] albumArtBytes = retriever.getEmbeddedPicture();
            return albumArtBytes != null && albumArtBytes.length > 0;

        } catch (Exception e) {
            Log.e(TAG, "Error checking album art in: " + filePath + " - " + e.getMessage());
            return false;
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever: " + e.getMessage());
            }
        }
    }
}

package de.zimbr0.loewi;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.OutputStream;

/**
 * Legt Dateien im öffentlichen Ordner "Download/Loewi" ab. Das geht seit Android 10
 * ohne Berechtigung (MediaStore) und die Dateien bleiben auch nach einer
 * Deinstallation der App erhalten – genau das braucht eine Sicherung.
 */
@CapacitorPlugin(name = "LoewiDatei")
public class LoewiDateiPlugin extends Plugin {

    private static final String ORDNER = "Loewi";

    @PluginMethod
    public void speichern(PluginCall call) {
        String name = call.getString("name");
        String mime = call.getString("mime", "application/octet-stream");
        String daten = call.getString("base64");
        boolean ueberschreiben = Boolean.TRUE.equals(call.getBoolean("ueberschreiben", false));
        if (name == null || name.isEmpty() || daten == null) {
            call.reject("name und base64 fehlen");
            return;
        }
        try {
            byte[] bytes = Base64.decode(daten, Base64.DEFAULT);
            ContentResolver cr = getContext().getContentResolver();
            String relativ = Environment.DIRECTORY_DOWNLOADS + "/" + ORDNER + "/";
            Uri sammlung = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

            Uri ziel = null;
            if (ueberschreiben) {
                ziel = vorhandene(cr, sammlung, name, relativ);
                if (ziel != null) {
                    try (OutputStream os = cr.openOutputStream(ziel, "wt")) {
                        if (os == null) throw new java.io.IOException("kein Stream");
                        os.write(bytes);
                    } catch (Exception e) {
                        /* Datei gehört nicht (mehr) dieser App, z. B. nach Neuinstallation: neu anlegen */
                        ziel = null;
                    }
                }
            }
            if (ziel == null) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, relativ);
                ziel = cr.insert(sammlung, cv);
                if (ziel == null) throw new java.io.IOException("Datei konnte nicht angelegt werden");
                try (OutputStream os = cr.openOutputStream(ziel)) {
                    if (os == null) throw new java.io.IOException("kein Stream");
                    os.write(bytes);
                }
            }
            JSObject res = new JSObject();
            res.put("pfad", "Download/" + ORDNER + "/" + name);
            call.resolve(res);
        } catch (Exception e) {
            call.reject("Speichern fehlgeschlagen: " + e.getMessage());
        }
    }

    private Uri vorhandene(ContentResolver cr, Uri sammlung, String name, String relativ) {
        String[] spalten = {MediaStore.MediaColumns._ID};
        String auswahl = MediaStore.MediaColumns.DISPLAY_NAME + "=? AND "
                + MediaStore.MediaColumns.RELATIVE_PATH + "=?";
        try (Cursor c = cr.query(sammlung, spalten, auswahl, new String[]{name, relativ}, null)) {
            if (c != null && c.moveToFirst()) {
                return Uri.withAppendedPath(sammlung, String.valueOf(c.getLong(0)));
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

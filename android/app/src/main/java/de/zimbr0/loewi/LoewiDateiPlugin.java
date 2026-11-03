package de.zimbr0.loewi;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.activity.result.ActivityResult;
import androidx.documentfile.provider.DocumentFile;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;

/**
 * Dateien für Sicherungen:
 *  - immer in den öffentlichen Ordner "Download/Loewi" (MediaStore, ohne Berechtigung,
 *    bleibt nach einer Deinstallation erhalten),
 *  - zusätzlich, wenn gewählt, in einen frei gewählten Ordner (Storage Access Framework),
 *    z. B. einen OneDrive-, Drive- oder SD-Karten-Ordner.
 */
@CapacitorPlugin(name = "LoewiDatei")
public class LoewiDateiPlugin extends Plugin {

    private static final String ORDNER = "Loewi";

    /** Installierte Fassung (versionCode = laufende Build-Nummer aus dem Workflow). */
    @PluginMethod
    public void version(PluginCall call) {
        try {
            PackageInfo pi = getContext().getPackageManager()
                    .getPackageInfo(getContext().getPackageName(), 0);
            JSObject res = new JSObject();
            long code = Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
            res.put("code", code);
            res.put("name", pi.versionName);
            call.resolve(res);
        } catch (Exception e) {
            call.reject("Version unbekannt: " + e.getMessage());
        }
    }

    /**
     * Lädt die neue APK in den App-Cache und startet den Android-Installer.
     * Fehlt die Erlaubnis "Apps aus dieser Quelle installieren", öffnet sich dafür die
     * Systemeinstellung, und der Aufruf meldet "erlaubnis" (danach erneut tippen).
     */
    @PluginMethod
    public void apkInstallieren(final PluginCall call) {
        final String adresse = call.getString("url");
        if (adresse == null || !adresse.startsWith("https://")) {
            call.reject("Adresse fehlt");
            return;
        }
        if (Build.VERSION.SDK_INT >= 26
                && !getContext().getPackageManager().canRequestPackageInstalls()) {
            Intent e = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getContext().getPackageName()));
            e.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(e);
            call.reject("erlaubnis");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File ordner = new File(getContext().getCacheDir(), "update");
                    if (!ordner.exists() && !ordner.mkdirs()) throw new IOException("Ordner");
                    File ziel = new File(ordner, "Loewi.apk");
                    HttpURLConnection c = (HttpURLConnection) new URL(adresse).openConnection();
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(30000);
                    c.setInstanceFollowRedirects(true);
                    if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
                    try (InputStream in = c.getInputStream();
                         FileOutputStream out = new FileOutputStream(ziel)) {
                        byte[] puffer = new byte[16384];
                        int n;
                        while ((n = in.read(puffer)) > 0) out.write(puffer, 0, n);
                    }
                    Uri uri = FileProvider.getUriForFile(getContext(),
                            getContext().getPackageName() + ".fileprovider", ziel);
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri, "application/vnd.android.package-archive");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(i);
                    call.resolve();
                } catch (Exception e) {
                    call.reject("Download fehlgeschlagen: " + e.getMessage());
                }
            }
        }).start();
    }

    /** Öffnet den Android-Ordnerdialog und merkt sich die Berechtigung dauerhaft. */
    @PluginMethod
    public void ordnerWaehlen(PluginCall call) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(call, i, "ordnerErgebnis");
    }

    @ActivityCallback
    private void ordnerErgebnis(PluginCall call, ActivityResult result) {
        if (call == null) return;
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null
                || result.getData().getData() == null) {
            call.reject("abgebrochen");
            return;
        }
        Uri uri = result.getData().getData();
        try {
            getContext().getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            DocumentFile d = DocumentFile.fromTreeUri(getContext(), uri);
            JSObject res = new JSObject();
            res.put("uri", uri.toString());
            res.put("name", d != null && d.getName() != null ? d.getName() : "Ordner");
            call.resolve(res);
        } catch (Exception e) {
            call.reject("Ordner nicht nutzbar: " + e.getMessage());
        }
    }

    /**
     * Speichert eine Datei. Download/Loewi und (falls "ordner" gesetzt) der gewählte Ordner
     * werden unabhängig voneinander beschrieben; der Aufruf scheitert nur, wenn beides misslingt.
     */
    @PluginMethod
    public void speichern(PluginCall call) {
        String name = call.getString("name");
        String mime = call.getString("mime", "application/octet-stream");
        String daten = call.getString("base64");
        String ordner = call.getString("ordner");
        boolean ueberschreiben = Boolean.TRUE.equals(call.getBoolean("ueberschreiben", false));
        if (name == null || name.isEmpty() || daten == null) {
            call.reject("name und base64 fehlen");
            return;
        }
        byte[] bytes;
        try {
            bytes = Base64.decode(daten, Base64.DEFAULT);
        } catch (Exception e) {
            call.reject("Daten nicht lesbar");
            return;
        }

        JSObject res = new JSObject();
        boolean downloadOk = false;
        String downloadFehler = null;
        try {
            inDownloads(bytes, name, mime, ueberschreiben);
            downloadOk = true;
            res.put("pfad", "Download/" + ORDNER + "/" + name);
        } catch (Exception e) {
            downloadFehler = String.valueOf(e.getMessage());
        }
        res.put("downloadOk", downloadOk);

        boolean ordnerOk = false;
        if (ordner != null && !ordner.isEmpty()) {
            try {
                inOrdner(Uri.parse(ordner), bytes, name, mime, ueberschreiben);
                ordnerOk = true;
            } catch (Exception e) {
                res.put("ordnerFehler", String.valueOf(e.getMessage()));
            }
            res.put("ordnerOk", ordnerOk);
        }

        if (!downloadOk && !ordnerOk) {
            call.reject("Speichern fehlgeschlagen: " + downloadFehler);
            return;
        }
        call.resolve(res);
    }

    private void inDownloads(byte[] bytes, String name, String mime, boolean ueberschreiben)
            throws IOException {
        ContentResolver cr = getContext().getContentResolver();
        String relativ = Environment.DIRECTORY_DOWNLOADS + "/" + ORDNER + "/";
        Uri sammlung = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

        Uri ziel = null;
        if (ueberschreiben) {
            ziel = vorhandene(cr, sammlung, name, relativ);
            if (ziel != null) {
                try (OutputStream os = cr.openOutputStream(ziel, "wt")) {
                    if (os == null) throw new IOException("kein Stream");
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
            if (ziel == null) throw new IOException("Datei konnte nicht angelegt werden");
            try (OutputStream os = cr.openOutputStream(ziel)) {
                if (os == null) throw new IOException("kein Stream");
                os.write(bytes);
            }
        }
    }

    private void inOrdner(Uri baum, byte[] bytes, String name, String mime, boolean ueberschreiben)
            throws IOException {
        DocumentFile wurzel = DocumentFile.fromTreeUri(getContext(), baum);
        if (wurzel == null || !wurzel.exists() || !wurzel.canWrite()) {
            throw new IOException("Ordner nicht erreichbar");
        }
        ContentResolver cr = getContext().getContentResolver();
        DocumentFile vorhanden = wurzel.findFile(name);
        if (vorhanden != null) {
            if (ueberschreiben) {
                try (OutputStream os = cr.openOutputStream(vorhanden.getUri(), "wt")) {
                    if (os == null) throw new IOException("kein Stream");
                    os.write(bytes);
                    return;
                } catch (Exception e) {
                    /* Anbieter erlaubt kein Überschreiben: alte Datei löschen, neu anlegen */
                    if (!vorhanden.delete()) throw new IOException("Datei nicht ersetzbar");
                }
            }
        }
        DocumentFile neu = wurzel.createFile(mime, name);
        if (neu == null) throw new IOException("Datei konnte nicht angelegt werden");
        try (OutputStream os = cr.openOutputStream(neu.getUri())) {
            if (os == null) throw new IOException("kein Stream");
            os.write(bytes);
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

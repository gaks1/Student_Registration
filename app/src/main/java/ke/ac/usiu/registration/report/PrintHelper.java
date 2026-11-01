package ke.ac.usiu.registration.report;

import android.app.Activity;
import android.content.Context;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Prints HTML through Android's print framework. The print dialog also offers
 * "Save as PDF", so it works even when no physical printer is available.
 */
public final class PrintHelper {

    // Keep a reference so the WebView is not garbage-collected before printing starts.
    private static WebView keepAlive;

    private PrintHelper() {
    }

    public static void print(Activity activity, String jobName, String html, boolean landscape) {
        WebView webView = new WebView(activity);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager pm = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
                if (pm == null) {
                    return;
                }
                PrintAttributes.MediaSize size = landscape
                        ? PrintAttributes.MediaSize.ISO_A4.asLandscape()
                        : PrintAttributes.MediaSize.ISO_A4;
                PrintAttributes attrs = new PrintAttributes.Builder().setMediaSize(size).build();
                pm.print(jobName, view.createPrintDocumentAdapter(jobName), attrs);
            }
        });
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);
        keepAlive = webView;
    }
}

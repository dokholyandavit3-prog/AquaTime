package david.dokholyan.aquatime.ui.champions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar; // Добавили импорт

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import david.dokholyan.aquatime.R;

public class AthleteDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_athlete_detail, container, false);

        WebView webView = view.findViewById(R.id.webView);
        // Используем View для кнопки назад, чтобы не было ошибки приведения типов (CastException)
        View back = view.findViewById(R.id.btn_back);
        ProgressBar loader = view.findViewById(R.id.web_loader);

        if (back != null) {
            back.setOnClickListener(v -> requireActivity().onBackPressed());
        }

        if (webView != null) {
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            // Добавляем поддержку хранилища для корректной работы Википедии
            webSettings.setDomStorageEnabled(true);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                    if (loader != null) loader.setVisibility(View.VISIBLE);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    if (loader != null) loader.setVisibility(View.GONE);
                }
            });

            if (getArguments() != null) {
                String url = getArguments().getString("url", "https://wikipedia.org");
                webView.loadUrl(url);
            }
        }

        return view;
    }
}
package david.dokholyan.aquatime.ui.champions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

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
        Button back = view.findViewById(R.id.btn_back);

        if (back != null) {
            back.setOnClickListener(v -> requireActivity().onBackPressed());
        }

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        if (getArguments() != null) {
            String url = getArguments().getString("url", "https://wikipedia.org");
            webView.loadUrl(url);
        }

        return view;
    }
}

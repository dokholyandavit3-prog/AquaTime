package david.dokholyan.aquatime.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.List;

import david.dokholyan.aquatime.R;

public class PoolsMapFragment extends Fragment {

    private MapView mapView;
    private MapObjectCollection mapObjects;
    private SharedPreferences sharedPreferences;
    private UserLocationLayer userLocationLayer;
    private boolean isEnglish;

    private final List<PoolData> allPools = new ArrayList<>();
    private final List<PlacemarkMapObject> markerList = new ArrayList<>();

    public static final String PREFS_NAME = "AquaTime";
    public static final String KEY_FAVORITE_POOL = "FavoritePool";

    private final MapObjectTapListener mapObjectTapListener = (mapObject, point) -> {
        if (mapObject instanceof PlacemarkMapObject && mapObject.getUserData() != null) {
            String poolName = (String) mapObject.getUserData();
            showPoolDetailsDialog(poolName);
            return true;
        }
        return false;
    };


    private static class PoolData {
        Point point;
        String name;

        PoolData(Point point, String name) {
            this.point = point;
            this.name = name;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_pools_map, container, false);

        mapView = v.findViewById(R.id.mapview);
        Button btnBack = v.findViewById(R.id.btn_back);

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isEnglish = getString(R.string.nav_home).equals("Home");

        if (btnBack != null) {
            btnBack.setOnClickListener(view -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        if (mapView != null) {
            mapObjects = mapView.getMap().getMapObjects();


            userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapView.getMapWindow());
            userLocationLayer.setVisible(true);
            userLocationLayer.setHeadingEnabled(true);

            mapView.postDelayed(() -> {
                if (mapView != null && mapView.getMap() != null) {
                    mapView.getMap().move(
                            new CameraPosition(new Point(40.4000, 44.5000), 8.5f, 0.0f, 0.0f),
                            new Animation(Animation.Type.SMOOTH, 1.5f),
                            null
                    );

                    initPoolsList();
                    addSwimmingPoolsToMap();
                }
            }, 150);
        }

        return v;
    }

    private void initPoolsList() {
        allPools.clear();

        // --- ЕРЕВАН ---
        allPools.add(new PoolData(new Point(40.146581, 44.498115), "Grand Sport"));

        String ambartsumyanName = isEnglish ? "David Hambardzumyan Swimming Pool" : "Бассейн им. Давида Амбарцумяна";
        allPools.add(new PoolData(new Point(40.167732, 44.523821), ambartsumyanName));

        allPools.add(new PoolData(new Point(40.205602, 44.498933), "DDD Sport Complex"));
        allPools.add(new PoolData(new Point(40.179720, 44.522511), "Multi Wellness Center"));
        allPools.add(new PoolData(new Point(40.210515, 44.450388), "Cross Sport Complex"));
        allPools.add(new PoolData(new Point(40.199182, 44.573210), "Hills Sport Complex"));
        allPools.add(new PoolData(new Point(40.181512, 44.490184), "Orange Fitness premium club"));
        allPools.add(new PoolData(new Point(40.204780, 44.496522), "Gold's Gym Armenia"));

        // --- РЕГИОНЫ ---
        String olympicName = isEnglish ? "Olympic Sports Complex (Tsaghkadzor)" : "Olympic Sports Complex (Цахкадзор)";
        allPools.add(new PoolData(new Point(40.531744, 44.720612), olympicName));

        String hrazdanName = isEnglish ? "Hrazdan Swimming Pool" : "Бассейн в Раздане";
        allPools.add(new PoolData(new Point(40.497911, 44.758489), hrazdanName));

        String vanadzorName = isEnglish ? "Vanadzor Swimming Pool" : "Ванадзорский бассейн";
        allPools.add(new PoolData(new Point(40.784534, 44.482431), vanadzorName));

        String gyumriName = isEnglish ? "Gyumri Swimming Pool" : "Гюмрийский бассейн";
        allPools.add(new PoolData(new Point(40.789410, 43.847490), gyumriName));

        String abovyanName = isEnglish ? "Abovyan Swimming Pool" : "Абовянский бассейн";
        allPools.add(new PoolData(new Point(40.271012, 44.633481), abovyanName));
    }

    private void addSwimmingPoolsToMap() {
        if (mapObjects == null) return;

        mapObjects.clear();
        markerList.clear();

        String favoritePool = sharedPreferences.getString(KEY_FAVORITE_POOL, "");
        Point userLocation = (userLocationLayer != null && userLocationLayer.cameraPosition() != null)
                ? userLocationLayer.cameraPosition().getTarget() : null;

        String closestPoolName = findClosestPool(userLocation);

        for (PoolData pool : allPools) {
            createPoolMarker(pool.point, pool.name, favoritePool, pool.name.equals(closestPoolName));
        }
    }


    private String findClosestPool(Point userLocation) {
        if (userLocation == null) return "";

        String closestName = "";
        double minDistance = Double.MAX_VALUE;

        for (PoolData pool : allPools) {
            double latDiff = pool.point.getLatitude() - userLocation.getLatitude();
            double lonDiff = pool.point.getLongitude() - userLocation.getLongitude();
            double distance = Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);

            if (distance < minDistance) {
                minDistance = distance;
                closestName = pool.name;
            }
        }
        return closestName;
    }

    private void createPoolMarker(Point point, String name, String favoritePool, boolean isClosest) {
        int iconResource;

        if (name.equals(favoritePool)) {
            iconResource = android.R.drawable.presence_invisible;
        } else if (isClosest) {
            iconResource = android.R.drawable.presence_away;
        } else {
            iconResource = android.R.drawable.presence_online;
        }

        ImageProvider icon = ImageProvider.fromResource(requireContext(), iconResource);

        PlacemarkMapObject placemark = mapObjects.addPlacemark(point, icon);
        placemark.setUserData(name);
        placemark.setIconStyle(new IconStyle().setAnchor(new android.graphics.PointF(0.5f, 0.5f)));
        placemark.addTapListener(mapObjectTapListener);

        markerList.add(placemark);
    }

    private void updateMarkerColors() {
        String favoritePool = sharedPreferences.getString(KEY_FAVORITE_POOL, "");
        Point userLocation = (userLocationLayer != null && userLocationLayer.cameraPosition() != null)
                ? userLocationLayer.cameraPosition().getTarget() : null;

        String closestPoolName = findClosestPool(userLocation);

        for (PlacemarkMapObject placemark : markerList) {
            String name = (String) placemark.getUserData();
            if (name == null) continue;

            int iconResource;
            if (name.equals(favoritePool)) {
                iconResource = android.R.drawable.presence_invisible;
            } else if (name.equals(closestPoolName)) {
                iconResource = android.R.drawable.presence_away;
            } else {
                iconResource = android.R.drawable.presence_online;
            }

            placemark.setIcon(ImageProvider.fromResource(requireContext(), iconResource));
            placemark.setIconStyle(new IconStyle().setAnchor(new android.graphics.PointF(0.5f, 0.5f)));
        }
    }

    private void showPoolDetailsDialog(String poolName) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_pool_details, null);
        bottomSheetDialog.setContentView(dialogView);

        TextView tvName = dialogView.findViewById(R.id.tv_pool_name);
        ImageButton btnFavorite = dialogView.findViewById(R.id.btn_favorite);

        tvName.setText(poolName);

        String favoritePool = sharedPreferences.getString(KEY_FAVORITE_POOL, "");
        final boolean[] isFavorite = {poolName.equals(favoritePool)};

        if (isFavorite[0]) {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
        }

        btnFavorite.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (isFavorite[0]) {
                editor.remove(KEY_FAVORITE_POOL);
                btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
                isFavorite[0] = false;
            } else {
                editor.putString(KEY_FAVORITE_POOL, poolName);
                btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
                isFavorite[0] = true;
            }
            editor.apply();

            updateMarkerColors();
        });

        bottomSheetDialog.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    public void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }
}
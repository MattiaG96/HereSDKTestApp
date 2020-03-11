package es.mattiagarreffa.heresdktestapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.here.sdk.core.Anchor2D;
import com.here.sdk.core.GeoCircle;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.Metadata;
import com.here.sdk.core.TextFormat;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.mapviewlite.MapImage;
import com.here.sdk.mapviewlite.MapImageFactory;
import com.here.sdk.mapviewlite.MapMarker;
import com.here.sdk.mapviewlite.MapMarkerImageStyle;
import com.here.sdk.mapviewlite.MapPolyline;
import com.here.sdk.mapviewlite.MapPolylineStyle;
import com.here.sdk.mapviewlite.MapScene;
import com.here.sdk.mapviewlite.MapStyle;
import com.here.sdk.mapviewlite.MapViewLite;
import com.here.sdk.mapviewlite.PixelFormat;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.Waypoint;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchError;
import com.here.sdk.search.SearchOptions;
import com.here.sdk.search.SearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainActivityTwo extends AppCompatActivity {

    SearchEngine searchEngine;
    RoutingEngine routingEngine;
    MapPolyline previousPolyline = null;
    int length;
    List<String> restaurants = new ArrayList<>();
    Button searchButton;
    private MapViewLite mapView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivityTwo.this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION},
                1);

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show_restaurants_dialog();
            }
        });

        loadMapScene();

    }

    private void loadMapScene() {
        // Load a scene from the SDK to render the map with a map style.
        mapView.getMapScene().loadScene(MapStyle.NORMAL_DAY, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapScene.ErrorCode errorCode) {
                if (errorCode == null) {
                    mapView.getCamera().setTarget(new GeoCoordinates(52.530858, 13.384744));
                    mapView.getCamera().setZoomLevel(14);

                    addMapMarkers(new GeoCoordinates(52.530858, 13.384744), R.drawable.poimine, null);
                    addMapMarkers(new GeoCoordinates(52.65465, 12.58196), R.drawable.poitest, null);

                    get_route();

                    //search_restaurants(lat, lon);
                } else {

                    System.out.println("onLoadScene failed: " + errorCode.toString());
                }
            }
        });
    }

    private void addMapMarkers(GeoCoordinates geoCoordinates, int image, Metadata metadata) {
        MapImage mapImage = MapImageFactory.fromResource(MainActivityTwo.this.getResources(), image);
        MapMarker mapMarker = new MapMarker(geoCoordinates);

        MapMarkerImageStyle mapMarkerImageStyle = new MapMarkerImageStyle();
        mapMarkerImageStyle.setAnchorPoint(new Anchor2D(0.5F, 1));

        mapMarker.addImage(mapImage, mapMarkerImageStyle);
        mapMarker.setMetadata(metadata);
        mapView.getMapScene().addMapMarker(mapMarker);
    }

    private void get_route() {
        try {
            routingEngine = new RoutingEngine();
        } catch (InstantiationErrorException e) {
            System.out.println("Initialization of RoutingEngine failed: " + e.error.name());
        }

        Waypoint startWaypoint = new Waypoint(new GeoCoordinates(52.530858, 13.384744));
        Waypoint destinationWaypoint = new Waypoint(new GeoCoordinates(52.65465, 12.58196));

        List<Waypoint> waypoints = new ArrayList<>(Arrays.asList(startWaypoint, destinationWaypoint));

        routingEngine.calculateRoute(waypoints, new RoutingEngine.CarOptions(), (routingError, routes) -> {
            if (routingError == null) {
                Route route = Objects.requireNonNull(routes).get(0);
                System.out.println(">>>>>>>>>>>>>>>>>>>>ROUTE: " + route);
                length = route.getLengthInMeters();
                showRouteOnMap(route);
                search_restaurants(52.530858, 13.384744, length / 2);
                search_restaurants(52.65465, 12.58196, length / 2);
            } else {
                System.out.println("Error while calculating a route:" + routingError.toString());
            }
        });
    }

    private void showRouteOnMap(Route route) {
        if (previousPolyline != null) {
            mapView.getMapScene().removeMapPolyline(previousPolyline);
        }
        GeoPolyline routeGeoPolyline;
        try {
            routeGeoPolyline = new GeoPolyline(route.getPolyline());
        } catch (InstantiationErrorException e) {
            return;
        }
        MapPolylineStyle mapPolylineStyle = new MapPolylineStyle();
        mapPolylineStyle.setColor(0x00908AA0, PixelFormat.RGBA_8888);
        mapPolylineStyle.setWidth(15);
        MapPolyline routeMapPolyline = new MapPolyline(routeGeoPolyline, mapPolylineStyle);
        previousPolyline = routeMapPolyline;
        mapView.getMapScene().addMapPolyline(routeMapPolyline);

    }

    private void search_restaurants(double lat1, double lon1, int metres) {
        try {
            searchEngine = new SearchEngine();
        } catch (InstantiationErrorException e) {
            System.out.println("Initialization of SearchEngine failed: " + e.error.name());
        }

        int maxSearchResults = 50;
        SearchOptions searchOptions = new SearchOptions(
                LanguageCode.EN_US,
                TextFormat.PLAIN,
                maxSearchResults);

        GeoCircle geoCircle = new GeoCircle(new GeoCoordinates(lat1, lon1), metres);
        searchEngine.search("restaurant", geoCircle, searchOptions, new SearchEngine.Callback() {
            @Override
            public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<SearchResult> list) {
                if (searchError != null) {
                    System.out.println("Error: " + searchError.toString());
                    return;
                }

                if (Objects.requireNonNull(list).isEmpty()) {
                    System.out.println("Search No results found");
                } else {
                    System.out.println("Results: " + list.size());
                }

                // Add new marker for each search result on map.
                for (SearchResult searchResult : list) {

                    restaurants.add(searchResult.title);
                }

                System.out.println(">>>>>>>>>>>>>>>> " + restaurants);
            }
        });
    }

    private void show_restaurants_dialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setCancelable(true);
        alertDialog.setIcon(R.mipmap.ic_launcher_round);
        alertDialog.setTitle("Restaurants");

        ListView listView = new ListView(this);

        alertDialog.setView(listView);

        alertDialog.show();

        listView.setAdapter(new ListAdapter() {
            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int position) {
                return false;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public int getCount() {
                return restaurants.size();
            }

            @Override
            public Object getItem(int position) {
                return position;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @SuppressLint("ViewHolder")
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                convertView = getLayoutInflater().inflate(R.layout.restaurant_cell, parent, false);

                TextView textView = convertView.findViewById(R.id.textView);
                textView.setText(restaurants.get(position));

                return convertView;
            }

            @Override
            public int getItemViewType(int position) {
                return position;
            }

            @Override
            public int getViewTypeCount() {
                if (restaurants.isEmpty()) {
                    return 1;
                } else {
                    return restaurants.size();
                }
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        });
    }
}

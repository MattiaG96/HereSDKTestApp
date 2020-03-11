package es.mattiagarreffa.heresdktestapp;

import android.Manifest;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.here.sdk.core.Anchor2D;
import com.here.sdk.core.GeoCircle;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.Metadata;
import com.here.sdk.core.Point2D;
import com.here.sdk.core.TextFormat;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.gestures.TapListener;
import com.here.sdk.mapviewlite.MapImage;
import com.here.sdk.mapviewlite.MapImageFactory;
import com.here.sdk.mapviewlite.MapMarker;
import com.here.sdk.mapviewlite.MapMarkerImageStyle;
import com.here.sdk.mapviewlite.MapPolyline;
import com.here.sdk.mapviewlite.MapPolylineStyle;
import com.here.sdk.mapviewlite.MapScene;
import com.here.sdk.mapviewlite.MapStyle;
import com.here.sdk.mapviewlite.MapViewLite;
import com.here.sdk.mapviewlite.PickMapItemsCallback;
import com.here.sdk.mapviewlite.PickMapItemsResult;
import com.here.sdk.mapviewlite.PixelFormat;
import com.here.sdk.routing.CalculateRouteCallback;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.RoutingError;
import com.here.sdk.routing.Waypoint;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchError;
import com.here.sdk.search.SearchOptions;
import com.here.sdk.search.SearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private MapViewLite mapView;
    SearchEngine searchEngine;
    RoutingEngine routingEngine;
    FusedLocationProviderClient fusedLocationClient;
    MapPolyline previousPolyline = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION},
                1);

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>" + location.getLatitude() + " - " + location.getLongitude());
                    // Get a MapViewLite instance from the layout.

                    loadMapScene(location.getLatitude(), location.getLongitude());
                    setTapGestureHandler(location.getLatitude(), location.getLongitude());
                }
            }
        });
    }

    private void loadMapScene(double lat, double lon) {
        // Load a scene from the SDK to render the map with a map style.
        mapView.getMapScene().loadScene(MapStyle.NORMAL_DAY, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapScene.ErrorCode errorCode) {
                if (errorCode == null) {
                    mapView.getCamera().setTarget(new GeoCoordinates(lat, lon));
                    mapView.getCamera().setZoomLevel(14);

                    addMapMarkers(new GeoCoordinates(lat, lon), R.drawable.poimine, null);

                    search_restaurants(lat, lon);
                } else {

                    System.out.println("onLoadScene failed: " + errorCode.toString());
                }
            }
        });
    }

    private void addMapMarkers(GeoCoordinates geoCoordinates, int image, Metadata metadata) {
        MapImage mapImage = MapImageFactory.fromResource(MainActivity.this.getResources(), image);
        MapMarker mapMarker = new MapMarker(geoCoordinates);

        MapMarkerImageStyle mapMarkerImageStyle = new MapMarkerImageStyle();
        mapMarkerImageStyle.setAnchorPoint(new Anchor2D(0.5F, 1));

        mapMarker.addImage(mapImage, mapMarkerImageStyle);
        mapMarker.setMetadata(metadata);
        mapView.getMapScene().addMapMarker(mapMarker);
    }

    private void search_restaurants(double lat1, double lon1) {
        try {
            searchEngine = new SearchEngine();
        } catch (InstantiationErrorException e) {
            System.out.println("Initialization of SearchEngine failed: " + e.error.name());
        }

        int maxSearchResults = 30;
        SearchOptions searchOptions = new SearchOptions(
                LanguageCode.EN_US,
                TextFormat.PLAIN,
                maxSearchResults);

        GeoCircle geoCircle = new GeoCircle(new GeoCoordinates(lat1, lon1), 5000);
        searchEngine.search("restaurant", geoCircle, searchOptions, new SearchEngine.Callback() {
            @Override
            public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<SearchResult> list) {
                if (searchError != null) {
                    System.out.println("Error: " + searchError.toString());
                    return;
                }

                if (list.isEmpty()) {
                    System.out.println("Search No results found");
                } else {
                    System.out.println("Results: " + list.size());
                }

                // Add new marker for each search result on map.
                for (SearchResult searchResult : list) {
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>RESULT: " + searchResult.coordinates.latitude + "-" + searchResult.coordinates.longitude + ">>>>>>>>>> " + searchResult.title);
                    Metadata metadata = new Metadata();
                    metadata.setString("key_poi", searchResult.title);

                    addMapMarkers(new GeoCoordinates(searchResult.coordinates.latitude, searchResult.coordinates.longitude), R.drawable.poitest, metadata);
                }
            }
        });
    }

    private void setTapGestureHandler(double lat, double lon) {
        mapView.getGestures().setTapListener(new TapListener() {
            @Override
            public void onTap(Point2D touchPoint) {
                pickMapMarker(touchPoint, lat, lon);
            }
        });
    }

    private void pickMapMarker(final Point2D touchPoint, double lat, double lon) {
        float radiusInPixel = 2;
        mapView.pickMapItems(touchPoint, radiusInPixel, new PickMapItemsCallback() {
            @Override
            public void onMapItemsPicked(@Nullable PickMapItemsResult pickMapItemsResult) {
                if (pickMapItemsResult == null) {
                    return;
                }

                MapMarker topmostMapMarker = pickMapItemsResult.getTopmostMarker();
                if (topmostMapMarker == null) {
                    return;
                }

                Toast.makeText(MainActivity.this, "MapMarker Picked", Toast.LENGTH_SHORT).show();
                Metadata metadata = topmostMapMarker.getMetadata();
                if (metadata != null) {
                    String message = "No message found.";
                    String string = metadata.getString("key_poi");
                    if (string != null) {
                        message = string;
                    }
                    Toast.makeText(getBaseContext(), "Map Marker picked " + message, Toast.LENGTH_LONG).show();
                    get_route(lat, lon, topmostMapMarker.getCoordinates().latitude, topmostMapMarker.getCoordinates().longitude);
                }
            }
        });
    }

    private void get_route(double startLat, double startLong, double endLat, double endLong) {
        System.out.println(">>>>>>>>>>>>>>>>>>>>COOR: " + startLat + " - " + startLong + " - " + endLat + " - " + endLong);
        try {
            routingEngine = new RoutingEngine();
        } catch (InstantiationErrorException e) {
            System.out.println("Initialization of RoutingEngine failed: " + e.error.name());
        }

        Waypoint startWaypoint = new Waypoint(new GeoCoordinates(startLat, startLong));
        Waypoint destinationWaypoint = new Waypoint(new GeoCoordinates(endLat, endLong));

        List<Waypoint> waypoints = new ArrayList<>(Arrays.asList(startWaypoint, destinationWaypoint));

        routingEngine.calculateRoute(waypoints, new RoutingEngine.CarOptions(), (routingError, routes) -> {
            if (routingError == null) {
                Route route = Objects.requireNonNull(routes).get(0);
                System.out.println(">>>>>>>>>>>>>>>>>>>>ROUTE: " + route);
                showRouteOnMap(route);
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
        mapPolylineStyle.setWidth(10);
        MapPolyline routeMapPolyline = new MapPolyline(routeGeoPolyline, mapPolylineStyle);
        previousPolyline = routeMapPolyline;
        mapView.getMapScene().addMapPolyline(routeMapPolyline);

    }

}

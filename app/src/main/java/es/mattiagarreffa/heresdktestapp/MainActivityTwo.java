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
import com.here.sdk.core.TextFormat;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.mapviewlite.MapImage;
import com.here.sdk.mapviewlite.MapImageFactory;
import com.here.sdk.mapviewlite.MapMarker;
import com.here.sdk.mapviewlite.MapMarkerImageStyle;
import com.here.sdk.mapviewlite.MapPolyline;
import com.here.sdk.mapviewlite.MapPolylineStyle;
import com.here.sdk.mapviewlite.MapStyle;
import com.here.sdk.mapviewlite.MapViewLite;
import com.here.sdk.mapviewlite.PixelFormat;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.Waypoint;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchOptions;
import com.here.sdk.search.SearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class MainActivityTwo extends AppCompatActivity {

    SearchEngine searchEngine; // Global SearchEngine
    RoutingEngine routingEngine; // Global RoutingEngine
    MapPolyline previousPolyline = null; // Global Polyline
    int length; // Global route length.
    List<String> restaurants = new ArrayList<>(); // Global restaurants array.
    Button searchButton; // Global search button
    private MapViewLite mapView; // Global mapView.

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        Here we have the onCreate method of the class.

        First of all we ask the user for the permissions to WRITE ON EXTERNAL STORAGE, READ EXTERNAL STORAGE, INTERNET, ACCESS NETWORK STATE, ACCESS COARSE LOCATION and ACCESS FINE LOCATION.

        After that we find the mapView.

        We do the same for the button which is going to search for the restaurants (searchButton).
        Then we create an onClick listener for the searchButton and we tell it to launch the show_restaurants_dialog() function which we will explain later.

        Right below the searchButton we call de loadMapScene() function we will explain this now.
         */

        setContentView(R.layout.activity_main);

        /*
        Below function ask for permissions.
         */
        ActivityCompat.requestPermissions(MainActivityTwo.this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION},
                1);

        mapView = findViewById(R.id.map_view); // This finds the view on the XML file.
        mapView.onCreate(savedInstanceState);

        searchButton = findViewById(R.id.searchButton); //This finds the searchButton on the XML file.
        searchButton.setOnClickListener(v -> show_restaurants_dialog()); //This creates the listener for the onClick action for the searchButton. It calls show_restaurants_dialog()

        loadMapScene(); // Call of the function loadMapScene().

    }

    private void loadMapScene() {

        /*
        The loadMapScene() it’s a very easy function that do not need any parameter and load for us and show a map on the MainScreen of the app.
        */

        /*
        The mapView.getMapScene().loadScene() is a method to prepare, load and create a map.
        The loadScene() method needs two parameters. The MapStyle (You can find them on HERE SDK documentation) and the Callback of the function.
        getMapScene() and loadScene() are HERE SDK methods.
         */
        mapView.getMapScene().loadScene(MapStyle.NORMAL_DAY, errorCode -> {
            /*
            Here we check if there is any error on the creation of the map.
             */
            if (errorCode == null) {
                /*
                If there is no error we use the mapView.getCamera().setTarget(); to pass in the coordinates of the place we want the map to focus on. It has
                one parameter, the GeoCoordinates() this built in method needs a latitude and a longitude to create the coordinates for the setTarget().

                After we call mapView.getCamera().setZoomLevel(14); to set the focus on the selected target coordinates whit a certain zoom level of your choice.
                 */
                mapView.getCamera().setTarget(new GeoCoordinates(52.530858, 13.384744));
                mapView.getCamera().setZoomLevel(14);

                addMapMarkers(new GeoCoordinates(52.530858, 13.384744), R.drawable.poimine); //Call for addMapMarkers() that adds a marker on the app. This one add a marker for the point A. It needs GeoCoordinates and an image to use it as a marker.
                addMapMarkers(new GeoCoordinates(52.65465, 12.58196), R.drawable.poitest); //Call for addMapMarkers() that adds a marker on the app. This one add a marker for the point A. It needs GeoCoordinates and an image to use it as a marker.

                get_route(); // Call for get_route(). It searches the route from point A to point B.
            } else {
                /*
                If there is an error we handle it as we want.
                 */
                System.out.println("onLoadScene failed: " + errorCode.toString());
            }
        });
    }

    private void addMapMarkers(GeoCoordinates geoCoordinates, int image) {
        /*
        This method needs two parameters, GeoCoordinates and a .png image from the resources of the app.
         */
        MapImage mapImage = MapImageFactory.fromResource(MainActivityTwo.this.getResources(), image); //HERE SDK method to get the image from the Drawable folder and create a marker.
        MapMarker mapMarker = new MapMarker(geoCoordinates); //HERE SDK method to set the marker on the desired coordinates.

        /*
        By default, each marker is centered on the location provided, and you may want to change this for some types of markers.
        An example is the POI marker, which usually points to the location with its bottom-middle position.

        We can achieve this with the mapMarkerImageStyle.setAnchorPoint(new Anchor2D());
         */
        MapMarkerImageStyle mapMarkerImageStyle = new MapMarkerImageStyle(); // We create an instance of the HERE SDK class MapMarkerImageStyle.
        mapMarkerImageStyle.setAnchorPoint(new Anchor2D(0.5F, 1)); // We set the anchor to the bottom-middle position to see the end of the poi marker on our position.

        mapMarker.addImage(mapImage, mapMarkerImageStyle); // This method add the newPosition to the marker.
        mapView.getMapScene().addMapMarker(mapMarker); //This method add the marker to the scene.
    }

    private void get_route() {
        /*
        This function show the route form point A to point B.
         */
        try {
            routingEngine = new RoutingEngine(); // We create a RoutingEngine instance.
        } catch (InstantiationErrorException e) {
            System.out.println("Initialization of RoutingEngine failed: " + e.error.name());
        }

        Waypoint startWaypoint = new Waypoint(new GeoCoordinates(52.530858, 13.384744)); // Whit this method we establish the coordinates for point A
        Waypoint destinationWaypoint = new Waypoint(new GeoCoordinates(52.65465, 12.58196)); // Whit this method we establish the coordinates for point B

        List<Waypoint> waypoints = new ArrayList<>(Arrays.asList(startWaypoint, destinationWaypoint)); // Whit this method we create a list for the routEngine so it can create the route.

        /*
        The function below calculate a route using the coordinates given before.
         */
        routingEngine.calculateRoute(waypoints, new RoutingEngine.CarOptions(), (routingError, routes) -> {
            if (routingError == null) {
                /*
                If there is no error.
                 */
                Route route = Objects.requireNonNull(routes).get(0); // We get the route object.
                System.out.println(">>>>>>>>>>>>>>>>>>>>ROUTE: " + route);
                length = route.getLengthInMeters(); // We get the route length.
                showRouteOnMap(route); // We show the route on the map.

                /*
                The two method below search for restaurants in a radius of a certain point.
                In the first call we pass in the coordinates of the point A and we divide the route length in half so we get all the restaurants along the first half of the route.
                In the second call we pass in the coordinates of the point B and we divide the route length in half so we get all the restaurants along the second half of the route.
                 */
                search_restaurants(52.530858, 13.384744, length / 2);
                search_restaurants(52.65465, 12.58196, length / 2);
            } else {
                /*
                If there is an error handle it as you prefer.
                 */
                System.out.println("Error while calculating a route:" + routingError.toString());
            }
        });
    }

    private void showRouteOnMap(Route route) {
        /*
        This function show a route on the map.
         */
        if (previousPolyline != null) {
            /*
            If there is already a route this method delete it.
             */
            mapView.getMapScene().removeMapPolyline(previousPolyline);
        }

        GeoPolyline routeGeoPolyline; //HERE SDK method to create a polyline.
        try {
            routeGeoPolyline = new GeoPolyline(route.getPolyline()); //HERE SDK method to get a polyline from the route calculated before.
        } catch (InstantiationErrorException e) {
            return;
        }

        MapPolylineStyle mapPolylineStyle = new MapPolylineStyle(); // This create a Style for the polyline.
        mapPolylineStyle.setColor(0x00908AA0, PixelFormat.RGBA_8888); // This changes the color of the Style.
        mapPolylineStyle.setWidth(15); // This changes the width of the style
        MapPolyline routeMapPolyline = new MapPolyline(routeGeoPolyline, mapPolylineStyle); // This gives the new style to the polyline.
        previousPolyline = routeMapPolyline; // This gives to the global variable previousPolyline the value of the actual polyline.
        mapView.getMapScene().addMapPolyline(routeMapPolyline); // This add the Polyline to the map.

    }

    private void search_restaurants(double lat1, double lon1, int metres) {
        /*
        This function search for restaurants in a radius of a certain point.
         */
        try {
            searchEngine = new SearchEngine(); // We create a searchEngine.
        } catch (InstantiationErrorException e) {
            System.out.println("Initialization of SearchEngine failed: " + e.error.name());
        }

        int maxSearchResults = 50; // Maximum number of restaurants per query.
        SearchOptions searchOptions = new SearchOptions(
                LanguageCode.EN_US, // Language options for the query
                TextFormat.PLAIN, // Format to receive the query results
                maxSearchResults);

        GeoCircle geoCircle = new GeoCircle(new GeoCoordinates(lat1, lon1), metres); // HERE SDK method to create a radius of certain metres using a given coordinates as centre.
        /*
        The below method needs three parameters.
        First one is a word or a list of words to make the query. In our case we use the word "restaurants"
        Second one is a place to make the query, in our case we use the GeoCircle we created before.
        Third one is the searchOptions.
         */
        searchEngine.search("restaurant", geoCircle, searchOptions, (searchError, list) -> {
            if (searchError != null) {
                /*
                If there is an error handle it as you prefer.
                 */
                System.out.println("Error: " + searchError.toString());
                return;
            }

            if (Objects.requireNonNull(list).isEmpty()) {
                /*
                If there is no restaurants in the area we trigger this.
                 */
                System.out.println("Search No results found");
            } else {
                /*
                If there are at least one restaurant in the area we trigger this.
                 */
                System.out.println("Results: " + list.size());
            }

            // Add new restaurant to the global array restaurants for each search result on map.
            for (SearchResult searchResult : list) {

                restaurants.add(searchResult.title);
            }

            System.out.println(">>>>>>>>>>>>>>>> " + restaurants);
        });
    }

    private void show_restaurants_dialog() {
        /*
        This function creates a dialog with a list of all the restaurants on the global array restaurants.
         */
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this); // Create the dialog.
        alertDialog.setCancelable(true); // Make the dialog cancellable so we do not need buttons to close it.
        alertDialog.setIcon(R.mipmap.ic_launcher_round); // set the icon for the dialog.
        alertDialog.setTitle("Restaurants"); // Set the title for the dialog.

        ListView listView = new ListView(this); // Creates a listView element.

        alertDialog.setView(listView); // Set the listView to be the view of the dialog.

        alertDialog.show(); // Show the dialog.

        /*
        The above method populate the list of the dialog.
         */
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

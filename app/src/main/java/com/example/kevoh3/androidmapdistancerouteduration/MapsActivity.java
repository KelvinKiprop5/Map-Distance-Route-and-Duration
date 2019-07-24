package com.example.kevoh3.androidmapdistancerouteduration;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.kevoh3.androidmapdistancerouteduration.Classes.DirectionObject;
import com.example.kevoh3.androidmapdistancerouteduration.Classes.GsonRequest;
import com.example.kevoh3.androidmapdistancerouteduration.Classes.Helper;
import com.example.kevoh3.androidmapdistancerouteduration.Classes.LegsObject;
import com.example.kevoh3.androidmapdistancerouteduration.Classes.PolylineObject;
import com.example.kevoh3.androidmapdistancerouteduration.Classes.RouteObject;
import com.example.kevoh3.androidmapdistancerouteduration.Classes.StepsObject;
import com.example.kevoh3.androidmapdistancerouteduration.Classes.VolleySingleton;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

//---------MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener-------------//
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener {

    //Declare variables
    private static final String TAG = MapsActivity.class.getSimpleName();

    private GoogleMap mMap;
    private List<LatLng> latLngList;
    private TextView distanceValue, durationValue;

    //-----------------------------------onCreate------------------------------//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//Allow screen change


        //Initialize variables
        distanceValue = (TextView) findViewById(R.id.distance_value);
        durationValue = (TextView) findViewById(R.id.duration_value);

        latLngList = new ArrayList<LatLng>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }//-----------------------------------./onCreate------------------------------//



    //-----------------------------------onMapReady------------------------------//
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);

        //Set marker here
        CameraPosition cameraPosition = CameraPosition.builder().target((new LatLng(-1.28333, 36.81667))).zoom(16).bearing(8).tilt(45).build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }//-----------------------------------./onMapReady------------------------------//


    //-----------------------------------Implemented methods------------------------------//
    @Override
    public void onMapClick(LatLng latLng) {

        if (latLngList.size() > 1){
            refreshMap(mMap);
            latLngList.clear();
            distanceValue.setText("");
            durationValue.setText("");
        }
        latLngList.add(latLng);
        Log.d(TAG, "Marker number "+latLngList.size());
        createMarker(latLng, latLngList.size());

        if (latLngList.size() == 2){
            LatLng origin = latLngList.get(0);
            LatLng destination = latLngList.get(1);


        //use Google Direction API to get the route between these Locations
        String directionApiPath = Helper.getUrl(String.valueOf(origin.latitude), String.valueOf(origin.longitude),
                String.valueOf(destination.latitude), String.valueOf(destination.longitude));
        Log.d(TAG, "Path " + directionApiPath);
        getDirectionFromDirectionApiServer(directionApiPath);
        }

    }//-----------------------------------./Implemented methods------------------------------//


    //-----------------------------------refreshMap------------------------------//
    private void refreshMap(GoogleMap mapInstance){
        mapInstance.clear();
    }//-----------------------------------./refreshMap------------------------------//


    //-----------------------------------createMarker------------------------------//
    private void createMarker(LatLng latLng, int position){
        MarkerOptions mOptions = new MarkerOptions().position(latLng);
        if(position == 1){
            mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        }else{
            mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }
        addCameraToMap(latLng);
        mMap.addMarker(mOptions);
    }//-----------------------------------./createMarker------------------------------//


    //-----------------------------------addCameraToMap------------------------------//
    private void addCameraToMap(LatLng latLng){
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(8)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }//-----------------------------------./addCameraToMap------------------------------//

    //-----------------------------------getDirectionFromDirectionApiServer------------------------------//
    private void getDirectionFromDirectionApiServer(String url){
        GsonRequest<DirectionObject> serverRequest = new GsonRequest <DirectionObject>(
                Request.Method.GET,
                url,
                DirectionObject.class,
                createRequestSuccessListener(),
                createRequestErrorListener());
        serverRequest.setRetryPolicy(new DefaultRetryPolicy(
                Helper.MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(serverRequest);
    } //-----------------------------------./getDirectionFromDirectionApiServer------------------------------//


    //-----------------------------------getDirectionFromDirectionApiServer------------------------------//
    private Response.Listener <DirectionObject> createRequestSuccessListener() {
        return new Response.Listener<DirectionObject>() {
            @Override
            public void onResponse(DirectionObject response) {
                try {
                    Log.d("JSON Response", response.toString());
                    if(response.getStatus().equals("OK")){
                        List <LatLng> mDirections = getDirectionPolylines(response.getRoutes());
                        drawRouteOnMap(mMap, mDirections);
                    }else{
                        Toast.makeText(MapsActivity.this, R.string.server_error, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
        };
    }//-----------------------------------./getDirectionFromDirectionApiServer------------------------------//


    //-----------------------------------setRouteDistanceAndDuration------------------------------//
    private void setRouteDistanceAndDuration(String distance, String duration){
        distanceValue.setText(distance);
        durationValue.setText(duration);
    }//-----------------------------------./setRouteDistanceAndDuration------------------------------//


    //-----------------------------------getDirectionPolylines------------------------------//
    private List <LatLng> getDirectionPolylines(List <RouteObject> routes){
        List <LatLng> directionList = new ArrayList <LatLng>();
        for(RouteObject route : routes){
            List <LegsObject> legs = route.getLegs();
            for(LegsObject leg : legs){
                String routeDistance = leg.getDistance().getText();
                String routeDuration = leg.getDuration().getText();
                setRouteDistanceAndDuration(routeDistance, routeDuration);
                List <StepsObject> steps = leg.getSteps();
                for(StepsObject step : steps){
                    PolylineObject polyline = step.getPolyline();
                    String points = polyline.getPoints();
                    List <LatLng> singlePolyline = decodePoly(points);
                    for (LatLng direction : singlePolyline){
                        directionList.add(direction);
                    }
                }
            }
        }
        return directionList;
    } //-----------------------------------./getDirectionPolylines------------------------------//



    //-----------------------------------Response.ErrorListener createRequestErrorListener------------------------------//
    private Response.ErrorListener createRequestErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        };
    } //-----------------------------------./Response.ErrorListener createRequestErrorListener------------------------------//

    //-----------------------------------.drawRouteOnMap------------------------------//
    private void drawRouteOnMap(GoogleMap map, List <LatLng> positions){
        PolylineOptions options = new PolylineOptions().width(5).color(Color.RED).geodesic(true);
        options.addAll(positions);
        Polyline polyline = map.addPolyline(options);
    } //-----------------------------------./drawRouteOnMap------------------------------//


    //--Method to decode polyline points Courtesy : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java-----//
    private List <LatLng> decodePoly(String encoded) {
        List <LatLng> poly = new ArrayList <>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b ^ 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
 //--./Method to decode polyline points Courtesy : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java-----//


}//---------./MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener-------------//

# weather-app
A simple weather app for Android that shows current weather and 12 hour forecast in user location. The app uses phone GPS for location (city). The data is queried from Finnish Meteorological Institute open data service. Temperature and weather type are shown in the app.

The app has three classes. The MainActivity class controls the UI, checks for permissions, calls GPS API for location, and stores the data.

ReverseGeocode takes the location as coordinates and calls Google maps Geocode API to get the corresponding city/municipality. This is done in a separate thread. Once finished, it calls the third class, FetchFMIData that opens a connection to FMI data service, makes a query for forecast, and reads the sent XML file. Temperature and weather symbol are stored. This runs as a background task. The UI is then populated with the stored weather data.

13.8.17 The app was updated to use Google maps Geocode API instead of Android Geocode class because the data given from the latter was incorrect in some cases.
/**
 * Created by shawn on 16/7/20.
 */

//= require jquery-2.2.0.min
//= require bootstrap
//= require mustache
//= require jquery.mustache
//= require_self

var map;
var map_clear;
var map_reload;
var map_query;

$(function() {
    var map_init = function(){
        map = new AMap.Map('mapContainer', {
            resizeEnable: true,
            center: mapConfig.centerCoordinate,
            zoom: mapConfig.mapZoomLevel
        });

        map.on('moveend', function(e){
            mapFilter['bounds'] = convertBounds(map.getBounds())
            //console.log("map resized, zoom: " + map.getZoom() + ", bounds: " + mapConfig.bounds);
            // Only reload when map zoom level > 5
            if(map.getZoom() > 5) {
                if(dataRequest){
                    dataRequest.abort();
                    dataRequest = null;
                    console.log("abort previous request");
                }
                console.log('moveend, auto reloading');
                map_reload(false);
            }else{
                PushService.subscribe();
            }
        });

        map.on('complete',function(e){
            mapFilter['bounds'] = convertBounds(map.getBounds())
            //console.log("map loaded, bounds: " + mapConfig.bounds);
            map_reload(true);
        });

        // Add map toolbar controller
        map.plugin( ["AMap.ToolBar"], function() {
            toolBar = new AMap.ToolBar();
            map.addControl(toolBar);
        });

        // Map type switcher
        map.plugin(["AMap.MapType"], function () {
            var mapType = new AMap.MapType({
                // by defult will be 2D
                // 1: sat map
                defaultType: 0,
                // add road layer
                showRoad: true
            });
            map.addControl(mapType);
        });

        // Map scale meters
        map.plugin(["AMap.Scale"], function () {
            scale = new AMap.Scale();
            map.addControl(scale);
        });

        // Load the templates
        $.Mustache.addFromDom('aprs_info_window_template');
        $.Mustache.addFromDom('info_window_template');
    }


    var convertBounds = function(bounds_in){
        var ne = bounds_in.getNorthEast();
        var sw = bounds_in.getSouthWest();
        return [ne.lat,sw.lng,sw.lat,ne.lng];
    }

    var infoWindow = new AMap.InfoWindow({
        // isCustom: true
        closeWhenClickMap:true,
    });

    var MultiPointRender = {
        pointsMap: {},
        render: function(points, tag) {
            if (points.length == 0) {
                return;
            };
            var marker;
            for (var i = 0; i < points.length; ++i) {
                point = points[i],
                    marker = new AMap.Marker({
                        icon: mapConfig.defaultMarkerIcon,
                        position: new AMap.LngLat(point[0], point[1])
                    });
                marker.setMap(map);
            }
            return marker;
        },
    };

    var PointRender = {
        pointsMap: {},
        render: function(point, udid, feature) {
            if (!point || !udid) {
                return;
            };
            var device_feature_properties = feature['properties'];
            var marker = this.pointsMap[udid]
            if(marker){
                //TODO check if mark needs invalidate
                if(device_feature_properties['marker-symbol'] == marker.extData['marker-symbol']){
                    // nothing to to if no symbol changes
                    marker.extData = device_feature_properties;
                    marker.moveTo(point, 1000);
                    return marker;
                }else{
                    // we'll setup the marker later
                }
            }

            if (!marker) {
                marker = new AMap.Marker({
                    icon: mapConfig.defaultMarkerIcon,
                    topWhenClick: true,
                    //animation:"AMAP_ANIMATION_DROP"
                });
                this.pointsMap[udid] = marker;
            }

            // Setup marker now
            marker.extData = device_feature_properties;
            var aprs = device_feature_properties['aprs'];
            if(typeof aprs != "undefined"){
                // For APRS marker, use UDID as label
                marker.setLabel({
                    offset:new AMap.Pixel(20,20),
                    content: "<div>" + device_feature_properties['udid'] + "</div>"
                });

                var markerIcon = new AMap.Icon(makeAprsIcon(device_feature_properties['marker-symbol']));
                /*
                // Nasty code for APRS Icons
                var symbol = device_feature_properties['marker-symbol'].split('_');
                var symbolIndex = parseInt(symbol[2]);
                var x = ((symbolIndex % 16) * (-24));
                var y = (Math.floor(symbolIndex / 16) * (-24));
                var markerIcon = new AMap.Icon({
                    size: [24,24],
                    imageOffset: new AMap.Pixel(x - 1,y - 1),
                    image: mapConfig.aprsMarkerImagePath + symbol[1] + "@2x.png",
                    imageSize:[384,144]
                });
                */
                marker.setIcon(markerIcon);

                // Setup Info Content View
                marker.on("click",showAprsInfoWindowOnMarkerClicked);
            }else{
                // For others, use username as label
                var username = device_feature_properties['username']
                marker.setLabel({
                    offset:new AMap.Pixel(12,25),
                    content: "<div>" + username + "</div>"
                });
                // Customized marker symbol
                var symbol = device_feature_properties['marker-symbol'];
                if(typeof symbol != "undefined"){
                    var markerIcon = new AMap.Icon({
                        //image : "http://webapi.amap.com/images/marker_sprite.png"
                        size: [32,32],
                        image : mapConfig.standardMakerImagePath + symbol + ".png",
                        imageSize: [32,32]
                    });
                    marker.setIcon(markerIcon);
                }
                marker.on("click",showStandardInfoWindowOnMarkerClicked);
            }
            marker.setPosition(point);
            marker.setMap(map);
            return marker;
        },
        clear:function(){
            var m = simpleCopy(this.pointsMap);
            this.pointsMap = {};
            console.log("cleaning markers");
            $.each(m,function(k,v){
                v.setMap(null);
            });
        }
    };

    var makeAprsIcon = function(symbolImage){
        var symbol = symbolImage.split('_');
        var symbolIndex = parseInt(symbol[2]);
        var x = ((symbolIndex % 16) * (-24));
        var y = (Math.floor(symbolIndex / 16) * (-24));
        var icon = {
            size: [24,24],
            imageOffset: new AMap.Pixel(x - 1,y - 1),
            image: mapConfig.aprsMarkerImagePath + symbol[1] + "@2x.png",
            imageSize:[384,144]
        };
        return icon;
    };

    var makeIcon = function(symbolImage){

    }

    var LineStringRender = {
        lineStringsMap: {},
        createLine: function(path_points, line_position_ids, udid){
            var polylineoptions = {
                path: path_points,
                strokeColor: "#3366FF",
                strokeOpacity: 0.8,
                strokeWeight: 4,
                strokeStyle: "solid",
                strokeDasharray: [100, 5]
            };
            // prepare the dots data
            var line = new AMap.Polyline(polylineoptions);

            if(mapConfig.showLineDots){
                // assume points.length > 0
                var dotsData = new Array();
                for(i in path_points){
                    var m = {lnglat:path_points[i],name:udid + "$" + line_position_ids[i]};
                    dotsData.push(m);
                }
                dotsData.shift();
                var dots = new AMap.MassMarks(dotsData,{
                    url: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png',
                    anchor: new AMap.Pixel(3, 7),
                    size: new AMap.Size(5, 7),
                    opacity:1.0,
                    cursor:'pointer',
                    alwaysRender:true,
                    zooms:[11],
                    zIndex: 999
                });
                dots.on('click', function(e){

                    infoWindow.setContent("Loading...");
                    infoWindow.open(map, new AMap.LngLat(e.data.lnglat['lng'], e.data.lnglat['lat']));
                    // console.log(e);

                    //var data = JSON.stringify({"udid": e.data['name']});
                    var data = {"udid": e.data['name']};
                    //FIXME - Correct API URL
                    var url = mapConfig.dataURL;
                    var urlparts= url.split('?'); // remove the parameters in dataURL
                    if (urlparts.length>=2) {
                        url = urlparts.shift();
                    }
                    $.get(url,data,function(result){
                        //console.log(result);
                        var marker = e.target;
                        var lnglat = e.data.lnglat;
                        var device_feature_properties = result['features'][0]['properties'];
                        marker.extData = device_feature_properties;
                        var aprs = device_feature_properties['aprs']
                        var event = {target: marker, lnglat: lnglat};
                        if(typeof aprs != "undefined") {
                            showAprsInfoWindowOnMarkerClicked(event);
                        }else{
                            showStandardInfoWindowOnMarkerClicked(event);
                        }
                    });
                });
                line.setExtData({dots:dots});
            }
            return line;
        },

        render: function(path_points, line_position_ids, udid) {
            if (typeof udid == "undefined"){
                return;
            }

            var polyline = this.lineStringsMap[udid];
            if(typeof polyline != "undefined"){
                // already created
                return;
            }

            if (path_points.length == 0) {
                return;
            };

            polyline = this.createLine(path_points,line_position_ids, udid);
            polyline.setMap(map);
            var dots = polyline.getExtData().dots;
            if(typeof dots != 'undefined'){
                dots.setMap(map);
            }
            // cache
            this.lineStringsMap[udid] = polyline;
            return polyline;
        },

        append: function(point, position_id, udid){
            if (typeof udid == "undefined"){
                return;
            }
            var path_points = new Array(point); // this is one point
            var line_position_ids = new Array(position_id); // the associated line position id
            var polyline = this.lineStringsMap[udid];
            if(typeof polyline == "undefined"){
                polyline = this.createLine(path_points,line_position_ids,udid);
                polyline.setMap(map);
                this.lineStringsMap[udid] = polyline;
            }else{
                // update the line
                path_points = polyline.getPath();
                path_points.unshift(point); // append current point to the top element of path array
                polyline.setPath(path_points);

                // update the dots
                var dots = polyline.getExtData().dots;
                if(typeof dots != 'undefined'){
                    var dotsData = dots.getData();
                    dotsData.unshift({lnglat:path_points[1],name:udid + "$" + position_id}); // the second dot in line
                    dots.setData(dotsData);
                }
            }
        },

        clear: function(){
            var m = simpleCopy(this.lineStringsMap);
            this.lineStringsMap = {};
            console.log("cleaning line");
            $.each(m,function(udid,line){
                var dots = line.getExtData().dots;
                if(dots){
                    dots.setData(null);
                    dots.setMap(null);
                    line.setExtData(null);
                }
                line.setMap(null);
            });

        }
    };

    var simpleCopy = function(obj){
        var newobj = {};
        for(var i in obj){
            newobj[i] = obj[i];
        }
        return newobj;
    };


    var showAprsInfoWindowOnMarkerClicked2 = function(event){
        // TODO - display power, gain,height
        infoWindow.setContent("");
        var device_feature_properties = event.target.extData;
        var lnglat = event.lnglat;

        var udid = device_feature_properties['udid'];
        var timestamp = device_feature_properties['timestamp'];
        var speed = device_feature_properties['speed'];
        var course = device_feature_properties['course'];
        var aprs = device_feature_properties['aprs'];

        var s = "<div class=\"marker-info\"><div><b>" + udid + "</b></div>";
        s = s.concat("<div> <hr color=\"blue\" size=\"1\"></hr>");
        s = s.concat("<div>",timestamp,"</div>");
        if((typeof speed != "undefined")){
            s = s.concat("<div><b>",speed," km/h ");
            if((typeof course != "undefined")){
                s = s.concat(course,"°");
            }
            s = s.concat("</b></div>");
        }
        s = s.concat("<div> <i><font color=\"green\">",aprs['comment'],"</font></i></div>");
        s = s.concat("<div>[",aprs['destination']," via ", aprs['path'],"]</div>");	// path
        s = s.concat("</div>");
        infoWindow.setContent(s);
        infoWindow.open(map, new AMap.LngLat(lnglat['lng'], lnglat['lat']));
    }

    var showAprsInfoWindowOnMarkerClicked = function(event){
        // TODO - display power, gain,height
        infoWindow.setContent("");
        var device_feature_properties = event.target.extData;
        var lnglat = event.lnglat;
        var icon = makeAprsIcon(device_feature_properties['marker-symbol'])
        var data = {
            'udid':device_feature_properties['udid'],
            'timestamp':device_feature_properties['timestamp'],
            'speed':device_feature_properties['speed'],
            'course':device_feature_properties['course'],
            'aprs':device_feature_properties['aprs'],
            'altitude':device_feature_properties['altitude'],
            'icon':icon,
        }
        var s = $.Mustache.render('aprs_info_window_template', data);
        infoWindow.setContent(s);
        infoWindow.open(map, new AMap.LngLat(lnglat['lng'], lnglat['lat']));
    }

    var showStandardInfoWindowOnMarkerClicked = function(event){
        infoWindow.setContent("");
        var device_feature_properties = event.target.extData;
        var lnglat = event.lnglat;
        var data={
            'udid':device_feature_properties['udid'],
            'username':device_feature_properties['username'],
            'timestamp':device_feature_properties['timestamp'],
            'phone':device_feature_properties['phone'],
            'speed':device_feature_properties['speed'],
            'course':device_feature_properties['course'],
            'message':device_feature_properties['message'],
        }
        var s = $.Mustache.render('info_window_template', data);
        infoWindow.setContent(s);
        infoWindow.open(map, new AMap.LngLat(lnglat['lng'], lnglat['lat']));
    }

    var showStandardInfoWindowOnMarkerClicked2 = function(event){
        infoWindow.setContent("");
        var device_feature_properties = event.target.extData;
        var lnglat = event.lnglat;

        var udid = device_feature_properties['udid'];
        var username = device_feature_properties['username'];
        var timestamp = device_feature_properties['timestamp'];
        var phone = device_feature_properties['phone'];
        var speed = device_feature_properties['speed'];
        var course = device_feature_properties['course'];
        var message = device_feature_properties['message'];

        var s = "<div class=\"marker-info\">"
        s = s.concat("<div>", username," ( <a href=\"tel:",phone,"\">",phone,"</a> )</div>");
        s = s.concat("<div> <hr color=\"blue\" size=\"1\"></hr></div>");
        s = s.concat("<div>设备:", udid, "</div>");
        s = s.concat("<div>时间:", timestamp, "</div>");
        if((typeof speed != "undefined")) {
            s = s.concat("<div>速度:", speed, " km/h ");
            if ((typeof course != "undefined")) {
                s = s.concat(course, "°");
            }
            s = s.concat("</div>");
        }
        if(typeof message != "undefined"){
            s = s.concat("<div>信息:",message,"</div>");
        }
        s = s.concat("</div>")

        infoWindow.setContent(s);
        infoWindow.open(map, new AMap.LngLat(lnglat['lng'], lnglat['lat']));
    }

    var parseGEOJSON = function(geojson) {
        if ("FeatureCollection" === geojson["type"]) {
            for (var i = 0; i < geojson.features.length; i++) {
                var feature = geojson.features[i];
                var udid = feature['properties']['udid'];
                var coordinates = feature['geometry']['coordinates'];
                var geometry_type = feature['geometry']['type'];
                if ("MultiPoint" === geometry_type) {
                    MultiPointRender.render(coordinates, udid);
                } else if ("Point" === geometry_type) {
                    var marker = PointRender.render(coordinates, udid, feature);
                    //setupDeviceMarker(marker, feature);
                } else if ("LineString" === geometry_type) {
                    var line_position_ids = feature['properties']['position_ids'];
                    LineStringRender.render(coordinates/*path_points*/, line_position_ids, udid);
                }
            };
            //map.setFitView();
        };
    };

    var updateGEOJSON = function(geojson) {
        if ("FeatureCollection" === geojson["type"]) {
            for (var i = 0; i < geojson.features.length; i++) {
                var feature = geojson.features[i];
                var udid = feature['properties']['udid'];
                var position_id = feature['properties']['position_id'];
                if ("Point" === feature['geometry']['type']) {
                    var current_point = feature['geometry']['coordinates'];
                    var marker = PointRender.render(current_point, udid, feature);
                    //setupDeviceMarker(marker, feature);

                    // Bounce the mark if no speed found
                    if(typeof feature['properties']['speed'] == "undefined" ){
                        // bounce those markers w/o speed info
                        marker.setAnimation("AMAP_ANIMATION_BOUNCE");
                        setTimeout(function() {
                            marker.setAnimation("");
                        }, 1200);
                    }

                    // update the line
                    LineStringRender.append(current_point,position_id,udid)
                }
            };
        };
    };

    var PushService = {
        websocket:null,
        running:0,
        connect:function(){
            if(this.websocket != null){
                return;
            }
            var self = this;
            try{
                this.websocket = new WebSocket(mapConfig.serviceURL);
                this.running = 1;
                console.log("websocket connected, url: " + mapConfig.serviceURL);
                this.websocket.onopen = function(e) {
                    //alert("PushService connected");
                    self.running = 2;
                    self.subscribe();
                };
                this.websocket.onclose = function(e) {
                    self.running = 0;
                    self.websocket = null;
                };
                this.websocket.onmessage = function(e) {
                    if(!self.running){
                        return;
                    }
                    var json = e.data;
                    // parse received json
                    try{
                        updateGEOJSON($.parseJSON(json));
                    }catch(err){
                        //if(debug) console.log(json);
                    }
                };
                this.websocket.onerror = function(e) {
                    self.disconnect(false);
                };
            }catch(err){
                //if(debug)console.log(err);
            }
            MapHeartBeat.start();
        },

        disconnect:function(stopHeartBeat) {
            if (stopHeartBeat) {
                MapHeartBeat.stop();
            }
            if(this.websocket != null){
                this.running = 0;
                this.websocket.close();
                this.websocket = null;
                console.log("websocket disconnected");
            }
        },

        ping:function(){
            this.send('PING');
        },

        send:function(message){
            if(this.websocket != null){
                this.websocket.send(message);
            }
        },

        isConnected:function(){
            return this.websocket != null && this.running === 2;
        },

        subscribe:function(){
            if(!this.isConnected()){
                console.log("websock not connected yet, subscribe aborted");
                return;
            }
            // send the filter
            var f = {filter:mapFilter};
            var s = JSON.stringify(f);
            this.send(s);
            console.log("updating filter: " + s);
        }
    };

    var MapHeartBeat = {
        heartBeatTimer:null,
        start:function(){
            //var start = new Date;
            if(this.heartBeatTimer){
                return;
            }
            this.heartBeatTimer = setInterval(function() {
                if(PushService.isConnected()){
                    //console.log("heartbeat ping...");
                    PushService.ping();
                }else{
                    //alert("Reconnecting websocket...");
                    PushService.connect();
                }
            }, 5000);
            console.log("heartbeat started");
        },
        stop:function(){
            if(this.heartBeatTimer){
                clearInterval(this.heartBeatTimer);
                this.heartBeatTimer = null;
                console.log("heartbeat stopped");
            }
        }
    };

    var dataRequest = null;
    // init entry point
    map_reload = function(init_load){
        if(dataRequest){
            dataRequest.abort();
            dataRequest = null;
            console.log("abort previous request");
        }

        // appending the map bounds to query parameter
        var data = {}
        if(mapFilter['udid']){
            data['udid'] = mapFilter['udid'];
        }
        if(mapFilter['type']){
            data['type'] = mapFilter['type'];
        }
        if(mapFilter['bounds']){
            data['bounds'] = mapFilter['bounds'].join(',');
        }

        if(init_load == true){
            PushService.disconnect(true);
        }
        console.log("map loading data from " + mapConfig.dataURL + " with params: " + JSON.stringify(data))
        dataRequest = $.ajax({
            url: mapConfig.dataURL,
            data:data,
            success: function(data){
                dataRequest = null;
                map_clear();
                parseGEOJSON(data);
                console.log("map load data completed")
                if(init_load == true) {
                    PushService.connect();
                }else{
                    PushService.subscribe(); // update the subscribe filter
                }
            },
            error: function(data,status){
                dataRequest = null;
            }
        });
    };

    map_clear = function(){
        // clear all makers
        PointRender.clear();
        // clear all lines
        LineStringRender.clear();
    }

    map_query = function(query){
        if(!query){
            return;
        }
        var url = mapConfig.queryURL;
        $.get(url,{q:query},function(data){
            var e = data['errorMessage'];
            if(e){
                //TODO - error alert
                alert("Error: " + e);
            }else if(data['mapConfig']){
                mapConfig = data['mapConfig'];
                mapFilter = data['mapFilter'];
                mapFilter['bounds'] = convertBounds(map.getBounds());
                //map_clear();
                if(map.center != mapConfig.centerCoordinate){
                    map.setCenter(mapConfig.centerCoordinate);
                    map.setZoom(10);
                    // will trigger reload after map moveend event
                }else{
                    // else force reload
                    map_reload(false);
                }
            }
        });
    }

    map_init()
});

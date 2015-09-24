<!DOCTYPE html>
<html>
	<head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        <meta content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0" name="viewport" />
        <meta name="screen-orientation" content="portrait">
        <meta name="x5-orientation" content="portrait">
        <title>
            ${mapConfig.title}
        </title>
        <style type="text/css">
            body{ margin:0; height:100%; width:100%; position:absolute; } 
            body *{padding: none; margin: none;} 
            #mapContainer{ position: absolute; top:0; left: 0; bottom:0; width:100%; } 
            /*
            #toolBox{ position: absolute; top:20px; right:0; bottom:0; width:20%; } 
            #submit{ position: absolute; top:0; right:0; width:20%; height: 22px;}
            */
            .amap-marker-label {
                padding: 0px 2px;
                border: 0px solid #666666;
                color: #222222;
                font: bold 12px arial,sans-serif;
            }
            .marker-info{
				padding:0px 0px 0px 0px;
				font: 10px arial,sans-serif;      
            }
        </style>
    </head>
    
    <body>
        <div id="mapContainer">
        </div>
        <script type="text/javascript" src="${createLink(uri:'/js/jquery-2.1.4.min.js')}">
        </script>
        <script type="text/javascript" src="http://webapi.amap.com/maps?v=1.3&key=cfce41430c43afbb7bd2cdfab2d9a2ee">
        </script>
        <script type="text/javascript" >
            $(function() {
                var dataURL = "<%=mapConfig.dataURL%>";
                var serviceURL = "<%=mapConfig.serviceURL%>";
                
                var map = new AMap.Map('mapContainer', {
                    resizeEnable: true,
                    center: [120.20, 30.24],
                    zoom: 11
                });

                var infoWindow = new AMap.InfoWindow({
                    // isCustom: true
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
                                icon: "http://webapi.amap.com/images/marker_sprite.png",
                                position: new AMap.LngLat(point[0], point[1])
                            });
                            marker.setMap(map);
                        }
                        //map.setFitView();
                        return marker;
                    },
                };

                var PointRender = {
                    pointsMap: {},
                    render: function(point, tag) {
                        if (!point) {
                            return;
                        };
                        var marker;
                        if (typeof tag == "undefined" || typeof this.pointsMap[tag] == "undefined") {
                            marker = new AMap.Marker({
                                icon: "http://webapi.amap.com/images/marker_sprite.png",
                                topWhenClick:true,
                            });
                            marker.setPosition(point);
                            marker.setMap(map);
                            if (tag) {
                                this.pointsMap[tag] = marker;
                            }
                        } else {
                            marker = this.pointsMap[tag];
                            marker && marker.setPosition(point);
                        }
                        //map.setFitView();
                        return marker;
                    },
                };

                var LineStringRender = {
                    lineStringsMap: {},
                    createLine: function(points){
                        var polylineoptions = {
                        	path: points,
                            strokeColor: "#3366FF", 
                            strokeOpacity: 1,       
                            strokeWeight: 5,        
                            strokeStyle: "solid",   
                            strokeDasharray: [10, 5]
                        };
                    	return new AMap.Polyline(polylineoptions);
                    },
                    
                    render: function(points, tag) {
                        if (points.length == 0) {
                            return;
                        };
                        var polyline;
                        if (typeof tag == "undefined" || typeof this.lineStringsMap[tag] == "undefined") {
                            polyline = this.createLine(points);
                            polyline.setMap(map);
                            if (tag) {
                                this.lineStringsMap[tag] = polyline;
                            };
                        } else {
                            polyline = this.lineStringsMap[tag];
                            polyline && polyline.setOptions(polylineoptions);
                        }
                        //map.setFitView();
                        return polyline;
                    },
                    
                    append: function(point, tag){
                    	if (typeof tag == "undefined"){
                    		return;
                    	}
                    	var path = new Array(point);
                    	var polyline = this.lineStringsMap[tag];
                    	if(typeof polyline == "undefined"){
                    		polyline = this.createLine(path);
                    		polyline.setMap(map);
                    		this.lineStringsMap[tag] = polyline;
                    	}else{
                    		//path = path.concat(polyline.getPath());
                    		path = polyline.getPath();
                    		path.unshift(point);
                    		polyline.setPath(path);
                    	}
                    	//path.unshift(point); // append current point to the top element of path array
                    	//polyline.show();
                    },
                }

                var setupCustomMarker = function(marker, feature) {
                	var message = feature['properties']['message'];
                	var symbol = feature['properties']['marker-symbol'];
                	var speed = feature['properties']['speed'];
                	var course = feature['properties']['course'];
                	var aprs = feature['properties']['aprs'];
                	if(typeof aprs != "undefined"){
                		// For APRS marker, use UDID as label
                        marker.setLabel({
                            offset:new AMap.Pixel(20,20),
                            content: "<div>" + feature['properties']['udid'] + "</div>"
                        });
                		
                		// Setup Info Content View
                		// TODO - display power, gain,height
                		// TODO - display speed and course
                        marker.on("click",function(e) {
                            infoWindow.setContent("");
                            
                            var s = "<div class=\"marker-info\"><div><b>" + feature['properties']['udid'] + "</b></div>";
                            s = s.concat("<div> <hr color=\"blue\" size=\"1\"></hr>");
                            s = s.concat("<div>",feature['properties']['timestamp'],"</div>");
                            // spped and course
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
                            infoWindow.open(map, new AMap.LngLat(e['lnglat']['lng'], e['lnglat']['lat']));
                        });
                		
                		// Nasty code for Icons
                        var symbol = aprs['symbol'].split('_');
                		var symbolIndex = parseInt(symbol[1]);
                		var x = ((symbolIndex % 16) * (-21));
                		var y = (Math.floor(symbolIndex / 16) * (-21)); 
                        var markerIcon = new AMap.Icon({
                        	size: [20,20],
                        	imageOffset: new AMap.Pixel(x - 1,y - 1),
                        	image: "${createLink(uri:'/images/aprs/sym')}" + symbol[0] + ".png"
                        });
                        marker.setIcon(markerIcon);
                	}else{
                		// For others, use username as label
                        marker.setLabel({
                            offset:new AMap.Pixel(12,25),
                            content: "<div>" + feature['properties']['username'] + "</div>"
                        });
                		
                        marker.on("click",function(e) {
                            infoWindow.setContent("");
                            var s = "<div class=\"marker-info\"> <div> 设备:" + feature['properties']['udid'] + "</div>";
                            s = s.concat("<div> 电话:<a href=\"tel:",feature['properties']['phone'],"\">",feature['properties']['phone'],"</a></div>");
                            
                            if((typeof speed != "undefined")){
                            	s = s.concat("<div>速度:",speed," km/h ");
                            	if((typeof course != "undefined")){
                            		s = s.concat(course,"°");
                            	}
                            	s = s.concat("</div>");
                            }
                            
                            if(typeof message != "undefined"){
                            	s = s.concat("<div>信息:",message,"</div>");
                            }
                            s = s.concat("</div>")
                            
                            infoWindow.setContent(s);
                            infoWindow.open(map, new AMap.LngLat(e['lnglat']['lng'], e['lnglat']['lat']));
                        });
                        
                        if(typeof symbol != "undefined"){
                            var markerIcon = new AMap.Icon({
                                //image : "http://webapi.amap.com/images/marker_sprite.png"
                                size: [48,48],
                            	image : "${createLink(uri:'/images/marker_')}" + feature['properties']['marker-symbol'] + ".png"
                            });
                            marker.setIcon(markerIcon);                        	
                        }
                	}
                };

                var parseGEOJSON = function(geojson) {
                    if ("FeatureCollection" === geojson["type"]) {
                        for (var i = 0; i < geojson.features.length; i++) {
                            var feature = geojson.features[i];
                            var tag = feature['properties']['udid'];
                            if ("MultiPoint" === feature['geometry']['type']) {
                                MultiPointRender.render(feature['geometry']['coordinates'], tag);
                            } else if ("Point" === feature['geometry']['type']) {
                                var marker = PointRender.render(feature['geometry']['coordinates'], tag);
                                setupCustomMarker(marker, feature);
                            } else if ("LineString" === feature['geometry']['type']) {
                                LineStringRender.render(feature['geometry']['coordinates'], tag);
                            }
                        };
                    };
                };
                
                
                
                var updateGEOJSON = function(geojson) {
                    if ("FeatureCollection" === geojson["type"]) {
                        for (var i = 0; i < geojson.features.length; i++) {
                            var feature = geojson.features[i];
                            var tag = feature['properties']['udid'];
                            if ("Point" === feature['geometry']['type']) {
                                var marker = PointRender.render(feature['geometry']['coordinates'], tag);
                                setupCustomMarker(marker, feature);
                                
                                // update the line
                                LineStringRender.append(feature['geometry']['coordinates'],tag)
                            }
                        };
                    };
                };

                var connectService = function(){
                    var wsUri = serviceURL;
                    websocket = new WebSocket(wsUri);
                    websocket.onopen = function(e) {
                        // alert("CONNECTED")
                    };
                    websocket.onclose = function(e) {
                        // alert("DISCONNECTED"); 
                    };
                    websocket.onmessage = function(e) { 
                        //parseGEOJSON($.parseJSON(e.data));
                        updateGEOJSON($.parseJSON(e.data))
                        //websocket.close(); 
                    };
                    websocket.onerror = function(e) { 
                        
                    };

                    function doSend(message) { 
                        websocket.send(message); 
                    }; 
                };
                
                var loadGeojson = function(){
                	$.get(dataURL,function(data) {
                		parseGEOJSON(data);
                		connectService();
                	});                	
                };
                loadGeojson();
                
            });

        </script>
    </body>
</html>

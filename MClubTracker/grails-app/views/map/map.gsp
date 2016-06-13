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
                opacity:0.6;
            }
            .marker-info{
				padding:0px 0px 0px 0px;
				font: 10px arial,sans-serif;      
            }
            .hamclub-copyrights{
            	margin-left:50%;
            	position: absolute;
    			right: 15px;
			    height: 16px;
			    bottom: 0;
			    font-size: 11px;
			    line-height: 16px;
			    font-family: Arial,sans-serif;
			    z-index: 160;    
            }
            .hamclub-site-license{

            }
        </style>
        <%--
        <script language="javascript" type="text/javascript">
			if(top.location!=self.location){
				// PLEASE DON'T INCLUDE ME!
				top.location="https://hamclub.net";
			}
		</script>
		--%>
    </head>
    
    <body>
        <div id="mapContainer">
        </div>
        <script type="text/javascript" src="${resource(dir:'js', file:'jquery-2.1.4.min.js')}">
        </script>
        <script type="text/javascript" src="${mapConfig.apiURL}">
        </script>

        <script type="text/javascript" >
            $(function() {
                var dataURL = "<%=mapConfig.dataURL%>";
                var serviceURL = "<%=mapConfig.serviceURL%>";
                var map = new AMap.Map('mapContainer', {
                    resizeEnable: true,
                    <%if(mapConfig.centerCoordinate){%>
                    center: <%=mapConfig.centerCoordinate.toString()%>,
					<%}%>
                    <%if(mapConfig.mapZoomLevel > 0){%>
                    zoom: <%=mapConfig.mapZoomLevel%>
                    <%}%>
                });

                var infoWindow = new AMap.InfoWindow({
                    // isCustom: true
                	closeWhenClickMap:true,
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
                                icon: "https://webapi.amap.com/images/marker_sprite.png",
                                position: new AMap.LngLat(point[0], point[1])
                            });
                            marker.setMap(map);
                        }
                        return marker;
                    },
                };

                var PointRender = {
                    pointsMap: {},
                    render: function(point, tag) {
                        if (!point || !tag) {
                            return;
                        };
                        var marker = this.pointsMap[tag]
                        if (typeof marker == "undefined") {
                            marker = new AMap.Marker({
                                icon: "https://webapi.amap.com/images/marker_sprite.png",
                                topWhenClick:true,
                            });
                            marker.setPosition(point);
                            marker.setMap(map);
                            if (tag) {
                                this.pointsMap[tag] = marker;
                            }
                        } else {
                            marker.moveTo(point,1000);
                        }
                        return marker;
                    },
                };

                var renderLineDots = <%=mapConfig.showLineDots%>
                var LineStringRender = {
                    lineStringsMap: {},
                    createLine: function(points){
                        var polylineoptions = {
                        	path: points,
                            strokeColor: "#3366FF", 
                            strokeOpacity: 0.8,       
                            strokeWeight: 4,        
                            strokeStyle: "solid",
                            strokeDasharray: [100, 5]
                        };
                        // prepare the dots data
                        var line = new AMap.Polyline(polylineoptions);

                        if(renderLineDots){
                            // assume points.length > 0
                            var dotsData = new Array();
                            for(i in points){
                                var m = {lnglat:points[i]/*,name:tag + i*/};
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
                            /*
                            var marksInfo = new AMap.Marker({
                                content:'',
                                map:map
                            });
                            marks.on('mouseover',function(e){
                                marksInfo.setPosition(e.data.lnglat);
                                marksInfo.setLabel({content:e.data.name});
                            });
                            */
                            line.setExtData({dots:dots});                            
                        }
                        return line;
                    },
                    
                    render: function(points, tag) {
                        if (points.length == 0) {
                            return;
                        };
                        var polyline = this.createLine(points);
                        if(typeof tag != "undefined"){
                            this.lineStringsMap[tag] = polyline;
                        }
                        polyline.setMap(map);
                        var dots = polyline.getExtData().dots;
                        if(typeof dots != 'undefined'){
                            dots.setMap(map);
                        }
                        return polyline;
                    },
                    
                    append: function(point, tag){
                    	if (typeof tag == "undefined"){
                    		return;
                    	}
                    	var path = new Array(point); // this is one point
                    	var polyline = this.lineStringsMap[tag];
                    	if(typeof polyline == "undefined"){
                    		polyline = this.createLine(path);
                    		polyline.setMap(map);
                    		this.lineStringsMap[tag] = polyline;
                    	}else{
                    		// update the line
                    		path = polyline.getPath();
                    		path.unshift(point); // append current point to the top element of path array
                    		polyline.setPath(path);

                            // update the dots
                            var dots = polyline.getExtData().dots;
                            if(typeof dots != 'undefined'){
                                var dotsData = dots.getData();
                                dotsData.unshift({lnglat:path[1]}); // the second dot in line
                                dots.setData(dotsData);
                            }                            
                    	}
                    },
                };

                var setupCustomMarker = function(marker, feature) {
                	var message = feature['properties']['message'];
                	var markerSymbol = feature['properties']['marker-symbol'];
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
                        marker.on("click",function(e) {
                            infoWindow.setContent("");
                            
                            var s = "<div class=\"marker-info\"><div><b>" + feature['properties']['udid'] + "</b></div>";
                            s = s.concat("<div> <hr color=\"blue\" size=\"1\"></hr></div>");
                            s = s.concat("<div>",feature['properties']['timestamp'],"</div>");
                            // speed and course
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
                		
                		// Nasty code for APRS Icons - Should read from aprs['symbol'] first ?
                        // var symbol = aprs['symbol'].split('_');
                		var symbol = feature['properties']['marker-symbol'].split('_');
                		var symbolIndex = parseInt(symbol[2]);
                		var x = ((symbolIndex % 16) * (-24));
                		var y = (Math.floor(symbolIndex / 16) * (-24)); 
                        var markerIcon = new AMap.Icon({
                        	size: [24,24],
                        	imageOffset: new AMap.Pixel(x - 1,y - 1),
                        	imageSize:[384,144],
                        	image: "${createLink(uri:'/static/images/aprs/aprs-fi-sym', absolute:false)}" + symbol[1] + "@2x.png"
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
                            var s = "<div class=\"marker-info\">"
                            s = s.concat("<div>", feature['properties']['username']," ( <a href=\"tel:",feature['properties']['phone'],"\">",feature['properties']['phone'],"</a> )</div>");
                            s = s.concat("<div> <hr color=\"blue\" size=\"1\"></hr></div>");
                            s = s.concat("<div>设备:", feature['properties']['udid'], "</div>");
                            s = s.concat("<div>时间:", feature['properties']['timestamp'], "</div>");                            
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
                        
                        if(typeof markerSymbol != "undefined"){
                            var markerIcon = new AMap.Icon({
                                size: [32,32],
                                imageSize: [32,32],
                                image : "${createLink(uri:'/static/images/map/', absolute:false)}" + feature['properties']['marker-symbol'] + ".png"
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

                var PushService = {
                	websocket:null,
                	connect:function(){
                		if(this.websocket != null){
                			return;
                		}
                		var self = this;
                		try{
                			this.websocket = new WebSocket(serviceURL);
                    		this.websocket.onopen = function(e) {
                    			//alert("PushService connected");
                            };
                            this.websocket.onclose = function(e) {
                            	self.websocket = null;
                            };
                            this.websocket.onmessage = function(e) {
                            	var json = e.data;
                            	// parse received json
                            	try{
                                   updateGEOJSON($.parseJSON(json));
                                }catch(err){
                                	//if(debug) console.log(json);
                                }
                            };
                            this.websocket.onerror = function(e) { 
                            	self.disconnect();
                            };
                		}catch(err){
                			//if(debug)console.log(err);
                		}
                	},
                	
                	disconnect:function(){
                		if(this.websocket != null){
                			this.websocket.close();
                			this.websocket = null;                			
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
                		return this.websocket != null;
                	},
                };
                
                var heartBeat = function(){
                	//var start = new Date;
                	setInterval(function() {
                	    //$('.Timer').text((new Date - start) / 1000 + " Seconds");
                	    //alert((new Date - start) / 1000 + " Seconds");
                	    if(PushService.isConnected()){
                	    	PushService.ping();
                	    }else{
                	    	//alert("Reconnecting websocket...");
                	    	PushService.connect();
                	    }
                	}, 5000);
                };

                // init entry point
               var init = function(){
                	$.get(dataURL,function(data) {
                		parseGEOJSON(data);
                		PushService.connect();
                		heartBeat();
                	});                	
                };
                init();
                //window.addEventListener("load",init,false);

            });

        </script>
        <%if(mapConfig.copyrights){%>
        <div class="hamclub-copyrights">${mapConfig.copyrights}</div>
        <%}%>
        <%if(mapConfig.siteLicense){%>
            <div class="hamclub-site-license"><a href="${mapConfig.siteLicenseLink}">${mapConfig.siteLicense}</a></div>
        <%}%>
    </body>
</html>

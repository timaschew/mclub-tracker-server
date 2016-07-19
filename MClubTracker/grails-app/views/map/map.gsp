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
            .hamclub-toolbar{
                width: 50%;
                display: table;
                margin-left: auto;
                margin-right: auto;
                margin-top:10px;
                font-size: 11px;
                line-height: 16px;
                font-family: Arial,sans-serif;
                z-index: 160;
                opacity:0.9;
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

        <%-- The JQuery Libs --%>
        <asset:javascript src="jquery-2.2.0.min.js"/>
        <%-- The Bootstrap Libs --%>
        <asset:javascript src="bootstrap.js"/>
        <asset:stylesheet src="bootstrap.css"/>
    </head>
    
    <body>
        <div id="mapContainer">
        </div>
        <script type="text/javascript" src="${mapConfig.mapApiURL}">
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
                    render: function(point, udid) {
                        if (!point || !udid) {
                            return;
                        };
                        var marker = this.pointsMap[udid]
                        if (typeof marker == "undefined") {
                            marker = new AMap.Marker({
                                icon: "https://webapi.amap.com/images/marker_sprite.png",
                                topWhenClick:true,
                            });
                            marker.setPosition(point);
                            marker.setMap(map);
                            this.pointsMap[udid] = marker;
                        } else {
                            marker.moveTo(point,1000);
                        }
                        return marker;
                    },
                };

                var renderLineDots = <%=mapConfig.showLineDots%>
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

                        if(renderLineDots){
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
                                var url = dataURL;
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
                        if (path_points.length == 0) {
                            return;
                        };
                        var polyline = this.createLine(path_points,line_position_ids, udid);
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
                };

                var showAprsInfoWindowOnMarkerClicked = function(event){
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

                var showStandardInfoWindowOnMarkerClicked = function(event){
                    infoWindow.setContent("");
                    var device_feature_properties = event.target.extData;
                    var lnglat = event.lnglat;

                    var udid = device_feature_properties['udid'];
                    var timestamp = device_feature_properties['timestamp'];
                    var speed = device_feature_properties['speed'];
                    var course = device_feature_properties['course'];
                    var phone = device_feature_properties['phone'];
                    var message = device_feature_properties['message'];

                    var s = "<div class=\"marker-info\"> <div> 设备:" + udid + "</div>";
                    s = s.concat("<div> 电话:<a href=\"tel:",phone,"\">",phone,"</a></div>");

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
                    infoWindow.open(map, new AMap.LngLat(lnglat['lng'], lnglat['lat']));
                }

                var setupDeviceMarker = function(marker, feature) {
                    var device_feature_properties = feature['properties'];
                    marker.extData = device_feature_properties;
                    var aprs = device_feature_properties['aprs'];
                    if(typeof aprs != "undefined"){
                        // For APRS marker, use UDID as label
                        marker.setLabel({
                            offset:new AMap.Pixel(20,20),
                            content: "<div>" + feature['properties']['udid'] + "</div>"
                        });

                        // Nasty code for APRS Icons
                        var symbol = feature['properties']['marker-symbol'].split('_');
                        var symbolIndex = parseInt(symbol[2]);
                        var x = ((symbolIndex % 16) * (-24));
                        var y = (Math.floor(symbolIndex / 16) * (-24));
                        var markerIcon = new AMap.Icon({
                            size: [24,24],
                            imageOffset: new AMap.Pixel(x - 1,y - 1),
                            image: "${assetPath(src: 'aprs/aprs-fi-sym')}" + symbol[1] + "@2x.png",
                            imageSize:[384,144]
                        });
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
                                image : "${assetPath(src: 'map/')}" + symbol + ".png",
                                imageSize: [32,32]
                            });
                            marker.setIcon(markerIcon);
                        }

                        marker.on("click",showStandardInfoWindowOnMarkerClicked);
                    }
                };

                var parseGEOJSON = function(geojson) {
                    if ("FeatureCollection" === geojson["type"]) {
                        for (var i = 0; i < geojson.features.length; i++) {
                            var feature = geojson.features[i];
                            var udid = feature['properties']['udid'];
                            var coordinates = feature['geometry']['coordinates'];
                            if ("MultiPoint" === feature['geometry']['type']) {
                                MultiPointRender.render(coordinates, udid);
                            } else if ("Point" === feature['geometry']['type']) {
                                var marker = PointRender.render(coordinates, udid);
                                setupDeviceMarker(marker, feature);
                            } else if ("LineString" === feature['geometry']['type']) {
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
                                var marker = PointRender.render(current_point, udid);
                                setupDeviceMarker(marker, feature);

                                // update the line
                                LineStringRender.append(current_point,position_id,udid)
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

        <!-- Search Form -->
        <div class="col-lg-6, hamclub-toolbar">
            <form name="query_form" id="query_form">
                <div class="input-group">
                    <input name="q" id="query_text" type="text" class="form-control" placeholder="搜索..." />
                    <span class="input-group-btn">
                        <button class="btn btn-default" type="button" id="search_button">Go!</button>
                    </span>
                </div><!-- /input-group -->
            </form>
        </div><!-- /.col-lg-6 -->
        <script>
            $('#search_button').click(function() {
                //var q = $("#query_text").val();
                //if(q.length > 0){
                    $("#query_form").submit();
                //}
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

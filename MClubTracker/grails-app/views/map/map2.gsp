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
        <asset:stylesheet src="map.css"/>
        <asset:stylesheet src="bootstrap-datepicker3.css"/>
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
        <div id="mapContainer"></div>
        <script type="text/javascript" src="${mapConfig.mapApiURL}"></script>

        <script type="text/javascript" >
            var mapConfig = {
                dataURL: "<%=mapConfig.dataURL%>",
                serviceURL: "<%=mapConfig.serviceURL%>",
                queryURL: "${mapConfig.queryURL}",
                aprsMarkerImagePath: "${mapConfig.aprsMarkerImagePath}",
                standardMakerImagePath: "${mapConfig.standardMakerImagePath}",
                centerCoordinate:<%=mapConfig.centerCoordinate.toString()%>,
                mapZoomLevel:<%=mapConfig.mapZoomLevel%>,
                showLineDots: <%=mapConfig.showLineDots%>,
                defaultMarkerIcon: "${mapConfig.defaultMarkerIcon}",
                deviceActiveDaysApi: "${mapConfig.deviceActiveDaysApi}",
                debug : ${mapConfig.debug},
            };

            var mapFilter = {
                udid: "${mapFilter.udid}",
                bounds:"${mapFilter.bounds}",
                mapId:"${mapFilter.mapId}",
                type:${mapFilter.type}
            };
        </script>


        <asset:javascript src="map2.js"/>
        <script type="text/javascript" >
        </script>

        <!-- Search Form -->
        <div class="col-lg-6, hamclub-toolbar">
            <form name="query_form" id="query_form" method="post">
                <div class="input-group">
                    <input name="q" id="query_text" type="text" class="form-control" placeholder="搜索 呼号/城市...""/>
                    <span class="input-group-btn">
                        <button class="btn btn-default" type="button" id="search_button">Go!</button>
                    </span>
                </div><!-- /input-group -->
            </form>
        </div><!-- /.col-lg-6 -->
        <script>
        $(function(){
            $('#search_button').click(function() {
                return submitQuery();
            });
            $('#query_form').submit(function(e){
                return submitQuery();
            });

            var submitQuery = function(){
                var q = $("#query_text").val();
                map_query(q);
                return false;
            }
        });
        </script>
    <%if(mapConfig.copyrights){%>
        <div class="hamclub-copyrights">${mapConfig.copyrights} v2</div>
        <%}%>
        <%if(mapConfig.siteLicense){%>
            <div class="hamclub-site-license"><a href="${mapConfig.siteLicenseLink}">${mapConfig.siteLicense}</a></div>
        <%}%>


    <%-- APRS InfoWindow Template --%>
    <script id="aprs_info_window_template" type="text/html">
        <div class="marker-info">
            <div class="marker-info-title">
                <span class="title-icon" style="background:url({{{icon.image}}}) no-repeat {{icon.imageOffset.x}}px {{icon.imageOffset.y}}px;background-size:{{icon.imageSize.0}}px {{icon.imageSize.1}}px;width:{{icon.size.0}}px;height:{{icon.size.1}}px"></span>
                <span><strong>{{udid}}</strong></span><!--<span class="title-links"><a href="#">跟踪</a></span>-->
                {{#historical}}<span><strong>(历史记录)</strong></span>{{/historical}}
            </div>
            <hr color="red" size="2" />
            <div class="marker-info-content">
                <div>{{timestamp}}</div>
                <div><b>
                    {{#speed}}速度:{{speed}} km/h{{/speed}}
                    {{#course}}方向:{{course}}°{{/course}}
                    {{#altitude}}高度:{{altitude}}m{{/altitude}}
                </b></div>
                <div><i><font color="green">{{aprs.comment}}</font></i></div>
                <div>[{{aprs.destination}} via {{aprs.path}}]</div>
            </div>
            {{#history}}
            <p/>
            <div>
                <span id="datepicker"><a href="#" id="history">历史轨迹(试用)</a></span>
            </div>
            {{/history}}
        </div>
    </script>

    <%-- Standard InfoWindow Template --%>
    <script id="info_window_template" type="text/html">
        <div class="marker-info">
            <div class="marker-info-title">
                <span>{{username}} (<a href="tel:{{phone}}">{{phone}}</a>)</span>
                {{#historical}}<span><strong>(历史记录)</strong></span>{{/historical}}
            </div>
            <hr color="blue" size="2" />
            <div class="marker-info-content">
                <div>设备:{{udid}}</div>
                <div>时间:{{timestamp}}</div>
                <div><b>
                    {{#speed}}速度:{{speed}} km/h{{/speed}}
                    {{#course}}方向:{{course}}°{{/course}}
                    {{#altitude}}高度:{{altitude}}m{{/altitude}}
                </b></div>
                <div>{{#message}}信息:{{message}}{{/message}}</div>
            </div>
            {{#history}}
            <p/>
            <div>
                <span id="datepicker"><a href="#" id="history">历史轨迹(试用)</a></span>
            </div>
            {{/history}}
        </div>
    </script>

    <asset:javascript src="history_datepicker.js"/>
    </body>
</html>

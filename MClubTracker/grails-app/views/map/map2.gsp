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
            .hamclub-ui{
                display: table;
                margin-left: auto;
                margin-right: auto;
                z-index: 160;
                opacity:0.99;
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
                defaultMarkerIcon: "${mapConfig.defaultMarkerIcon}"
            };

            var mapFilter = {
                udid: "${mapFilter.udid}",
                bounds:"${mapFilter.bounds}",
                mapId:"${mapFilter.mapId}"
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
        <div class="hamclub-copyrights">${mapConfig.copyrights}</div>
        <%}%>
        <%if(mapConfig.siteLicense){%>
            <div class="hamclub-site-license"><a href="${mapConfig.siteLicenseLink}">${mapConfig.siteLicense}</a></div>
        <%}%>
    </body>
</html>

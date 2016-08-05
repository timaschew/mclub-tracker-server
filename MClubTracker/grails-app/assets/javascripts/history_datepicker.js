/**
 * Created by shawn on 16/8/3.
 */
//= require bootstrap-datepicker.js
//= require bootstrap-datepicker.zh-CN.min.js
//= require_self

var currentDpDate = new Date();
var monthTable = [];
var datepickerShown = false;

var datepicker_init = function(){
    // hookup the history click event
    var datepicker = $('#datepicker');
    $("#history").click(function(e){
        if(!datepickerShown){
            console.log("will show datepicker");
            loadActiveDays(function(){
                $("#datepicker").datepicker('show')
            });
        }else{
            $("#datepicker").datepicker('hide')
            console.log("hide datepicker");
        }
        return false;
    });

    // initialize the datepicker
    datepicker.datepicker({
        //title: 'History',
        maxViewMode:'years',
        keyboardNavigation: false,
        autoclose: true,
        language: "zh-CN",
        endDate: new Date(),
        //defaultViewDate: { year: currentDpDate.getFullYear(), month: currentDpDate.getMonth(), day: currentDpDate.getDate() },
        beforeShowDay: function(date){
            var enabled = false;
            if (date.getMonth() == currentDpDate.getMonth()) {
                //enabled = true;
                // TODO - load available days from the server side
                for(i = 0; i < monthTable.length; i++){
                    //console.log("month table" + monthTable[i]);
                    if(date.getDate() === monthTable[i]){
                        enabled = true;
                        break;
                    }
                }
            }
            if(enabled){
                return 'highlighted';
                //return 'active';
            }else{
                return false;
            }
        },
    });


    datepicker
        .on("changeMonth", function (e) {
            console.log("Month changed to " + e.date);
            currentDpDate = new Date(e.date);
            // clear selected days
            monthTable = [];
            datepicker.datepicker('fill');
            // load from server for active days of the changed month
            loadActiveDays(function(){
                //$("#datepicker").datepicker('updateViewDate',currentDpDate);
                datepicker.datepicker('fill'); // redraw
            });
        })
        .on('show',function(e){
            datepickerShown = true;
            console.log("calendar show");
            // redraw the datepicker
            $('#datepicker').datepicker('fill');
        })
        .on('hide',function(e){
            datepickerShown = false;
            currentDpDate = new Date();
            console.log("calendar hide");
        })
        .on("changeDate", function (e) {
            if(!(e.date)){
                // day is cleared
                //console.log("Day cleared");
                return;
            }
            //currentDpDate = new Date(e.date);
            var date = new Date(e.date);
            console.log("Day changed to " + date);
            datepicker.datepicker('clearDates');

            // TODO - update filters and reload the map
            map_hide_info_window();
            mapFilter['udid'] = mapConfig['activeDevice'];
            var params = {
                'udid':mapConfig['activeDevice'],
                'historyTime':e.date.getFullYear() + "-" + (e.date.getMonth() + 1) + '-' + e.date.getDate(),
            };
            if(mapFilter['udid']){
                params['udid'] = mapFilter['udid'];
            }
            if(mapFilter['type']){
                params['type'] = mapFilter['type'];
            }
            if(mapFilter['bounds']){
                params['bounds'] = mapFilter['bounds'].join(',');
            }
            mapConfig['historical'] = true;
            map_reload(false,params);
        });

    var loadActiveDays = function(mycb){
        var data = {udid:mapConfig['activeDevice'],time:currentDpDate.getFullYear() + "-" + (currentDpDate.getMonth() + 1)};
        console.log("load active days, params: " + JSON.stringify(data));
        var dataRequest = $.ajax({
            //url: '/mtracker/api/report/device_active_days',/*mapConfig.historyURL,*/
            url: mapConfig.deviceActiveDaysApi,
            data:data,

            success: function(data){
                dataRequest = null;
                if(data.code === 0){
                    monthTable = [].concat(data.data);
                    console.log("active_days: " + monthTable);
                    if(mycb){
                        mycb();
                    }
                }else{
                    console.log("load active days failed, " + JSON.stringify(data));
                }
            },
            error: function(data,status){
                dataRequest = null;
                console.log("load active days failed, " + JSON.stringify(data) + ", status: " + status);
            }
        });
    }
}

/*
$(function() {
    $(document).off('.datepicker.data-api');
});
*/
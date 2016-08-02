package mclub.tracker

import grails.transaction.Transactional
import mclub.sys.ConfigService
import mclub.util.DateUtils

import java.text.SimpleDateFormat

@Transactional
class TrackerReportService {
    ConfigService configService;

    /**
     * List the days in one month of a device that with tracker records available
     * @param device the device report against
     * @param yearMonthString the 'yyyy-MM' formated year/month string
     * @return
     */
    def reportTheDaysInOneMonthOfTheDeviceThatWithTrackerRecordsAvailable(TrackerDevice device,String yearMonthString) {
        Date[] beginEnd = DateUtils.getBeginEndDayOfMonth(yearMonthString);
        if(!beginEnd){
            log.info("Invalid year/month string, ${yearMonthString}");
            return [];
        }
        // the query extracts day field from time column
        String hql = "SELECT DISTINCT extract(day from tp.time) FROM TrackerPosition AS tp WHERE tp.device=:device AND tp.time >= :begin AND tp.time <=:end"
        def result = TrackerPosition.executeQuery(hql,[device:device,begin:beginEnd[0],end:beginEnd[1]]);
        return result;
    }

    /**
     * List device position of day
     * @param deviceId
     * @param date
     * @return
     */
    public List<TrackerPosition> listDevicePositionOfDay(String udid, Date date){
        TrackerDevice device = TrackerDevice.findByUdid(udid);
        if(device){
            Date startTime = date;
            Date endTime = new Date(date.getTime() + mclub.util.DateUtils.TIME_OF_ONE_DAY);
            // query db for the date
            def results = TrackerPosition.findAll("FROM TrackerPosition p WHERE p.device=:dev AND p.time >=:startTime AND p.time <= :endTime",[dev:device, startTime:startTime, endTime:endTime, max:1500]);
            return results
        }else{
            log.info("Unknow device: ${udid}");
        }
        return [];
    }

    /**
     * List daily tracks.
     * We will save device positions into tracks automatically in daily basis.
     * @param deviceId
     * @param beginDay
     * @param endDay
     * @return
     */
    public List<TrackerTrack> listTracksBetweenDays(String udid, Date beginDay, Date endDay){
        def device = TrackerDevice.findByUdid(udid);
        if(!device){
            return [];
        }

        // List all tracks between that days
        def tracksInThatDays = TrackerTrack.findAll("FROM TrackerTrack tt WHERE tt.deviceId = :dev AND tt.beginDate >=:begin AND tt.beginDate <=:end",[dev:device.id, begin:beginDay, end:endDay]);
        return tracksInThatDays
    }
}

package com.cowbell.cordova.geofence;


import com.google.gson.annotations.Expose;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import android.util.Log;

public class Period {
    @Expose public Calendar fromDate;
    @Expose public Calendar toDate;
    @Expose public Calendar fromDateCurrentPeriod;
    @Expose public Calendar toDateCurrentPeriod;
    @Expose public int repeat;

    static final int ONCE        = 0;
    static final int EVERY_DAY   = 1;
    static final int EVERY_WEEK  = 2;
    static final int EVERY_MONTH = 3;
    static final int EVERY_YEAR  = 4;

    private Logger logger;

    public Period(Calendar fromDate, 
		  Calendar toDate, 
		  Calendar fromDateCurrentPeriod, 
		  Calendar toDateCurrentPeriod, 
		  int repeat) {
	this.fromDate              = fromDate;
	this.toDate                = toDate;
	this.fromDateCurrentPeriod = fromDateCurrentPeriod;
	this.toDateCurrentPeriod   = toDateCurrentPeriod;
	this.repeat                = repeat;
    }

    public boolean isRepeat() {
	return repeat != ONCE;
    }

    private boolean isWithin(Calendar fromDate, 
			     Calendar toDate,
			     Calendar now) {

        logger = Logger.getLogger();
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	logger.log(Log.DEBUG, "isWithin(): fromDate = " 
		    + sdf.format(fromDate.getTime()));
	logger.log(Log.DEBUG, "isWithin(): toDate   = "
		    + sdf.format(toDate.getTime()));
	logger.log(Log.DEBUG, "isWithin(): now      = "
		    + sdf.format(now.getTime()));
	logger.log(Log.DEBUG, "now.after(fromDate) = " + now.after(fromDate));
	logger.log(Log.DEBUG, "now.before(toDate) = " + now.before(toDate));

	if ((now.after(fromDate) == true) && (now.before(toDate) == true)) {
	    fromDateCurrentPeriod = fromDate;
	    toDateCurrentPeriod   = toDate;
	    return true;
	}
	return false;
    }

    private boolean isWithinOnce(Calendar now) {
	return isWithin(this.fromDate, this.toDate, now);
    }

    private boolean isWithinEveryDay(Calendar now) {
	int year, month, day;
	Calendar fromDate = this.fromDate;
	Calendar toDate   = this.toDate;

	year  = now.get(Calendar.YEAR);
	month = now.get(Calendar.MONTH);
	day   = now.get(Calendar.DAY_OF_MONTH);

	fromDate.set(year, month, day);
	toDate.set(year, month, day);

	return isWithin(fromDate, toDate, now);
    }

    private boolean isWithinEveryWeek(Calendar now) {
	int year, month, day;
	Calendar fromDate = this.fromDate;
	Calendar toDate   = this.toDate;

	if (fromDate.get(Calendar.DAY_OF_WEEK) !=
	    now.get(Calendar.DAY_OF_WEEK)) {
	    return false;
	}

	year  = now.get(Calendar.YEAR);
	month = now.get(Calendar.MONTH);
	day   = now.get(Calendar.DAY_OF_MONTH);

	fromDate.set(year, month, day);
	toDate.set(year, month, day);

	return isWithin(fromDate, toDate, now);
    }

    private boolean isWithinEveryMonth(Calendar now) {
	int year, month;
	Calendar fromDate = this.fromDate;
	Calendar toDate   = this.toDate;

	year  = now.get(Calendar.YEAR);
	month = now.get(Calendar.MONTH);

	fromDate.set(Calendar.YEAR, year);
	fromDate.set(Calendar.MONTH, month);
	toDate.set(Calendar.YEAR, year);
	toDate.set(Calendar.MONTH, month);

	return isWithin(fromDate, toDate, now);
    }

    private boolean isWithinEveryYear(Calendar now) {
	int year;
	Calendar fromDate = this.fromDate;
	Calendar toDate   = this.toDate;

	year  = now.get(Calendar.YEAR);

	fromDate.set(Calendar.YEAR, year);
	toDate.set(Calendar.YEAR, year);

	return isWithin(fromDate, toDate, now);
    }

    private boolean isWithin(Calendar now) {
        Logger logger = Logger.getLogger();
	boolean retval = false;
	switch (repeat) {
	case ONCE:
	    logger.log(Log.DEBUG, "ONCE");
	    retval = isWithinOnce(now);
	    break;
	case EVERY_DAY:
	    logger.log(Log.DEBUG, "EVERY_DAY");
	    retval = isWithinEveryDay(now);
	    break;
	case EVERY_WEEK:
	    logger.log(Log.DEBUG, "EVERY_WEEK");
	    retval = isWithinEveryWeek(now);
	    break;
	case EVERY_MONTH:
	    logger.log(Log.DEBUG, "EVERY_MONTH");
	    retval = isWithinEveryMonth(now);
	    break;
	case EVERY_YEAR:
	    logger.log(Log.DEBUG, "EVERY_YEAR");
	    retval = isWithinEveryYear(now);
	    break;
	default:
	    retval = false;
	    logger.log(Log.DEBUG, "Unknown state in repeat: " + repeat);
	    break;
	}
	return retval;
    }

    boolean isFiredInCurrentPeriod(Calendar now) {
        logger = Logger.getLogger();

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	logger.log(Log.DEBUG, "isFiredInCurrentPeriod(): fromDateCurrentPeriod = " 
		    + fromDateCurrentPeriod);
	logger.log(Log.DEBUG, "isFiredInCurrentPeriod(): toDateCurrentPeriod = " 
		    + toDateCurrentPeriod);
	
	logger.log(Log.DEBUG, "isFiredInCurrentPeriod(): fromDateCurrentPeriod = " 
		    + sdf.format(fromDateCurrentPeriod.getTime()));
	logger.log(Log.DEBUG, "isFiredInCurrentPeriod(): toDateCurrentPeriod   = "
		    + sdf.format(toDateCurrentPeriod.getTime()));
	logger.log(Log.DEBUG, "isFiredInCurrentPeriod(): now      = "
		    + sdf.format(now.getTime()));
	logger.log(Log.DEBUG, "now.after(fromDateCurrentPeriod) = " + now.after(fromDateCurrentPeriod));
	logger.log(Log.DEBUG, "now.before(toDateCurrentPeriod) = " + now.before(toDateCurrentPeriod));

	if ((now.after(fromDateCurrentPeriod) == true)
	    && (now.before(toDateCurrentPeriod) == true)) {
	    logger.log(Log.DEBUG, "isFiredInCurrentPeriod(): Already fired.");
	    return false;
	}
	if (isWithin(now) != true) {
	    logger.log(Log.DEBUG, "isFiredInCurrentPeriod(): isn't fired.");
	    return false;
	}
	logger.log(Log.DEBUG, "isFiredInCurrentPeriod(): fired.");
	return true;
    }

    public String toString() {
        return "Repeat fromDate: " + fromDate.toString()
            + " toDate: " + toDate.toString()
	    + " repeat: " + repeat
	    ;
    }
}

package com.cowbell.cordova.geofence;

import com.google.gson.annotations.Expose;
import java.util.Calendar;
import android.util.Log;

public class Period {
    @Expose public Calendar fromDate;
    @Expose public Calendar toDate;
    @Expose public Calendar type;
    @Expose public Repeat repeat;

    private Logger logger;

    public Period(Calendar fromDate, Calendar toDate, Repeat repeat) {
	this.fromDate = fromDate;
	this.toDate   = toDate;
	this.repeat   = repeat;
        logger = Logger.getLogger();
    }

    public boolean isRepeat() {
	return repeat != Repeat.ONCE;
    }

    private boolean isWithin(Calendar fromDate, 
			     Calendar toDate,
			     Calendar now) {
	if ((fromDate.after(now) == true) && (toDate.before(now) == true)) {
	    return true;
	}
	return false;
    }

    private boolean isWithinOnce(Calendar now) {
	return isWithin(this.fromDate, this.toDate, now);
    }

    private boolean isWithinEveryDay(Calendar now) {
	int year, month, day;
	Calendar fromDate = Calendar.getInstance();
	Calendar toDate   = Calendar.getInstance();

	year  = now.get(Calendar.YEAR);
	month = now.get(Calendar.MONTH);
	day   = now.get(Calendar.DAY_OF_MONTH);

	fromDate.set(year, month, day);
	toDate.set(year, month, day);

	return isWithin(fromDate, toDate, now);
    }

    private boolean isWithinEveryWeek(Calendar now) {
	int year, month, day;
	Calendar fromDate = Calendar.getInstance();
	Calendar toDate   = Calendar.getInstance();

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
	Calendar fromDate = Calendar.getInstance();
	Calendar toDate   = Calendar.getInstance();

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

	Calendar fromDate = Calendar.getInstance();
	Calendar toDate   = Calendar.getInstance();

	year  = now.get(Calendar.YEAR);

	fromDate.set(Calendar.YEAR, year);
	toDate.set(Calendar.YEAR, year);

	return isWithin(fromDate, toDate, now);
    }

    public boolean isWithin(Calendar now) {
        Logger logger = Logger.getLogger();
	boolean retval = false;
	switch (repeat) {
	case ONCE:
	    retval = isWithinOnce(now);
	    break;
	case EVERY_DAY:
	    retval = isWithinEveryDay(now);
	    break;
	case EVERY_WEEK:
	    retval = isWithinEveryWeek(now);
	    break;
	case EVERY_MONTH:
	    retval = isWithinEveryMonth(now);
	    break;
	case EVERY_YEAR:
	    retval = isWithinEveryYear(now);
	    break;
	default:
	    retval = false;
	    logger.log(Log.DEBUG, "Unknown state in repeat: " + repeat);
	    break;
	}
	return retval;
    }

    public String toString() {
        return "Repeat fromDate: " + fromDate.toString()
            + " toDate: " + toDate.toString()
	    + " repeat: " + repeat
	    ;
    }
}

package com.iservice.utils;

import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;



/**
 * @author Fellow Consulting
 */
public class DateUtils {

	public final static String FIRST_DATE = "first_date";
	public final static String LAST_DATE = "last_date";
	public final static String PREFIX_START_TIME = "T00:00:00Z";
	public final static String  PREFIX_END_TIME = "T23:59:59Z";

	public final static String OOD_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'.000Z'";
	public final static String CSV_DATE_TIME_FORMAT = "DD.MM.YYYY HH:mm:ss";

	public final static String DATABASE_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'.000Z'";
	public final static String DATABASE_DATETIME_FORMAT2 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	/**
	 * Date format to use for database. All dates in database must conform to
	 * that format.
	 */
	public final static String DATABASE_DATE_FORMAT = "yyyy-MM-dd";

	public static Date getDateInTimeZone(Date currentDate, TimeZone timeZone) {
		// System.out.println("getDateInTimeZone :" + timeZoneId);
		Calendar mbCal = new GregorianCalendar(timeZone);

		mbCal.setTime(currentDate);
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, mbCal.get(Calendar.YEAR));
		cal.set(Calendar.MONTH, mbCal.get(Calendar.MONTH));
		cal.set(Calendar.DAY_OF_MONTH, mbCal.get(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, mbCal.get(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, mbCal.get(Calendar.MINUTE));
		cal.set(Calendar.SECOND, mbCal.get(Calendar.SECOND));
		cal.set(Calendar.MILLISECOND, mbCal.get(Calendar.MILLISECOND));
		// System.out.println("cal.getTime() : " + cal.getTime());
		return cal.getTime();

	}

	public static Calendar parseStringAsCalender(String s) {
		Date d = parseString(s);
		Calendar c = Calendar.getInstance();
		c.setTime(d);

		return c;
	}

	public static Date parseString(String s, TimeZone tz) {
		try {
			return parseString(s, DATABASE_DATETIME_FORMAT, tz);
		} catch (Exception e) {
			return parseString(s, DATABASE_DATETIME_FORMAT2, tz);
		}
	}

	public static Date parseString(String s, String format, TimeZone tz) {
		if (s == null) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		if (tz != null) {
			sdf.setTimeZone(tz);
		}
		if (s.indexOf(":") < 0) {
			sdf = new SimpleDateFormat(new String("yyyy-MM-dd"));
		}

		// incoming data from oracle onDemand time is in UTC
		// sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			return sdf.parse(s);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

	}

	public static Date parseString(String s) {
		return parseString(s, TimeZone.getDefault());

	}

	public static String convertMinute(String s, int defaultMinute) {
		if (s == null) {
			return null;
		}
		Date d = parseString(s);
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.set(Calendar.MINUTE, defaultMinute);
		cal.set(Calendar.SECOND, 0);
		return DateUtils.format(cal.getTime(),
				DateUtils.DATABASE_DATETIME_FORMAT);
	}

	public static Date parse(String d, String pathern) {
		if (StringUtils.isEmpty(d)) {
			return null;
		}

		SimpleDateFormat sdf = new SimpleDateFormat(pathern);
		try {
			return sdf.parse(d);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public static String format(String date) {
		if (StringUtils.isEmpty(date)) {
			return "";
		}
		SimpleDateFormat dfTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date d = parseString(date);
			return dfTime.format(d);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static String guessDateFormat(String s) {
		if (s != null) {
			String delim = s.replaceAll("\\d", "");
			if (delim.equals("// ::")) {
				return "MM/DD/YYYY JJ:NN:SS";
			} else if (delim.equals("//")) {
				return "MM/DD/YYYY";
			}
		}
		return DATABASE_DATETIME_FORMAT;
	}

	public static Date guessAndParse(String s) {
		return parse(s, guessDateFormat(s));
	}

	public static String toDateTimeYMDHMS_PM(Date date) {
		return format(date, "YYYY/MM/DD") + " " + format(date, "L:MM:SS A");
	}

	public static String toDateTimeYMDHMS(Date date) {
		return format(date, "YYYY/MM/DD") + " " + format(date, "HH:MM:SS");
	}

	public static boolean isDate(String date) {
		if (date.length() < 8) {
			return false;
		}
		try {
			guessAndParse(date).toString();
			return true;
		} catch (Exception e) {
			// nothing to do
		}
		return false;
	}

	public static String format(Date date) {
		SimpleDateFormat dfTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			return dfTime.format(date);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static String getTime(String date) {
		SimpleDateFormat dfTime = new SimpleDateFormat("HH:mm:ss");
		SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			return dfTime.format(dfDate.parse(date));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static String format(String date, String format) {
		if (StringUtils.isEmpty(date)) {
			return "";
		}
		SimpleDateFormat dfTime = new SimpleDateFormat(format);
		try {
			Date d = parseString(date);
			return dfTime.format(d);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static String format(Date date, String format) {

		SimpleDateFormat dfTime = new SimpleDateFormat(format);
		try {
			return dfTime.format(date);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static String shortFormat(String date, String format) {
		if (StringUtils.isEmpty(date)) {
			return "";
		}
		SimpleDateFormat dfTime = new SimpleDateFormat(format);
		try {
			Date d = parseString(date);
			return dfTime.format(d);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	// public static String FORMAT_DATETIME = "MMMM dd, yyyy 'at' HH:mm aaa";

	public DateUtils() {

	}

	

	// public static String formatTime(Date date, String userid) {
	// SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATETIME);
	// Date zDate = getDateInTimeZone(date,getTimezoneId(userid));
	// return sdf.format(zDate);
	// }
	public static void main(String[] args) {
		try {
			String strDate = "2016-03-11T10:12:30Z";
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			sdf.format(new Date());
			Date d = sdf.parse(strDate);
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 1);
			c.set(Calendar.MILLISECOND, 0);
			System.out.println(sdf.format(c.getTime()));
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			sdf = new SimpleDateFormat(OOD_DATE_TIME_FORMAT);
			System.out.println(sdf.format(c.getTime()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String toIsoDate(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(OOD_DATE_TIME_FORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(date);
	}

	public static long formatTimeStamp(Date date) {
		return date.getTime();
	}

	public static String getStrDateTime(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(OOD_DATE_TIME_FORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(date);
	}

	public static String getStrDateTime(Calendar c) {
		return getStrDateTime(c, TimeZone.getTimeZone("GMT"));
	}

	public static String getStrDateTime(Calendar c, TimeZone tz) {
		SimpleDateFormat sdf = new SimpleDateFormat(OOD_DATE_TIME_FORMAT);
		sdf.setTimeZone(tz);
		return sdf.format(c.getTime());
	}

	public static String getLocalStrDateTime(Calendar c) {

		return getLocalStrDateTime(c, OOD_DATE_TIME_FORMAT);
	}

	public static String getLocalStrDateTime(Calendar c, String format) {
		if (c != null) {

			return getLocalStrDateTime(c.getTime(), format);
		}
		return null;
	}

	public static String getLocalStrDateTime(Date d, String format) {
		if (d != null) {
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			return sdf.format(d);
		}
		return null;
	}

	/**
	 *
	 * @param number
	 * @param date
	 * @param datepart
	 *            = "fullYear", "month", "date", "startDayOfWeek",
	 *            "endDayOfWeek"
	 * @return String format=DATABASE_DATETIME_FORMAT default
	 */
	public static String dateAdd(int number, Date date, String datepart,
			String format) {
		if (date == null) {
			/* Default to current date. */
			date = new Date();
		}

		Calendar returnDate = Calendar.getInstance();
		returnDate.setTime(date);

		switch (datepart.toLowerCase()) {
		case "fullyear":
			returnDate.add(Calendar.YEAR, number);
			break;
		case "month":
			returnDate.add(Calendar.MONTH, number);
			break;
		case "date":
			returnDate.add(Calendar.DATE, number);
			break;
		case "startdayofweek":

			int d = returnDate.get(Calendar.DATE)
			- returnDate.get(Calendar.DAY_OF_WEEK) - 1;

			returnDate.set(Calendar.DATE, d);
			break;
		case "enddayofweek":
			int ed = returnDate.get(Calendar.DATE) + 7
			- returnDate.get(Calendar.DAY_OF_WEEK);
			returnDate.set(Calendar.DATE, ed);
			break;
		default:
			/* Unknown date part, do nothing. */
			break;
		}
		return DateUtils.format(returnDate.getTime(), format);
	}

	public static Map<String, String> getFirstLastDateOfCurrentYear() {
		LocalDate now = LocalDate.now();
		LocalDate firstDate = now.with(firstDayOfYear());
		LocalDate lastDate = now.with(lastDayOfYear());
		Map<String, String> mStartEndDate = new HashMap<>();
		mStartEndDate.put(FIRST_DATE, firstDate.toString());
		mStartEndDate.put(LAST_DATE, lastDate.toString());
		return mStartEndDate;
	}

	public static String getDuration(String date) {
		List<String> lst = new ArrayList<>();
		long duration = getDateInTimeZone(Calendar.getInstance().getTime(),
				TimeZone.getTimeZone("GMT")).getTime()
				- (parseString(date).getTime() + 120000);
		long diffSeconds = duration / 1000 % 60;
		long diffMinutes = duration / (60 * 1000) % 60;
		long diffHours = duration / (60 * 60 * 1000) % 24;
		long diffDays = duration / (24 * 60 * 60 * 1000);

		if (diffDays > 0) {
			if (diffDays > 1) {
				lst.add(diffDays + " days");
			} else {
				lst.add(diffDays + " day");
			}
		}
		if (diffHours > 0) {
			if (diffHours > 1) {
				lst.add(diffHours + " hours");
			} else {
				lst.add(diffHours + " hour");
			}
		}
		if (diffMinutes > 0) {
			if (diffMinutes > 1) {
				lst.add(diffMinutes + " minutes");
			} else {
				lst.add(diffMinutes + " minute");
			}
		}
		if (diffSeconds > 0) {
			if (diffSeconds > 1) {
				lst.add(diffSeconds + " seconds");
			} else {
				lst.add(diffSeconds + " second");
			}
		}

		String strDuration = "";
		for (int i = 0; i < lst.size(); i++) {
			if (i == lst.size() - 1 && lst.size() > 1) {
				strDuration += " and " + lst.get(i);
			} else if (lst.size() > 0) {
				if (i == 0) {
					strDuration += lst.get(i);
				} else {
					strDuration += ", " + lst.get(i);
				}
			}

		}

		if (!strDuration.isEmpty()) {
			strDuration += " ago";
		}
		return strDuration;
	}
}

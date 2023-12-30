import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        List<String[]> data = getData();
        double[] realData = new double[data.size() - 1];
        Date[] time = new Date[data.size() - 1];

        for (int i = 1; i < data.size(); i++) {
            String[] row = data.get(i);
            time[i - 1] = parseDate(row[0]);
            realData[i - 1] = Double.parseDouble(row[1]);
        }

        double bestAlpha = 0;
        double bestRSME = Double.MAX_VALUE;
        for (double alpha = 0; alpha < 1; alpha += 0.1) {
            double[] forecastExp = exponentialSmoothing(realData, alpha);
            double newRSME = calculateRSME(subArray(realData, 1), subArray(forecastExp, 0, forecastExp.length - 1));
            if (newRSME < bestRSME) {
                bestAlpha = alpha;
                bestRSME = newRSME;
            }
        }

        int bestWindow = 0;
        bestRSME = Double.MAX_VALUE;
        for (int window = 5; window < 40; window++) {
            double[] forecastMovingAver = movingAverage(realData, window);
            double newRSME = calculateRSME(subArray(realData, window), forecastMovingAver);
            if (newRSME < bestRSME) {
                bestWindow = window;
                bestRSME = newRSME;
            }
        }

        double[] forecastExp = exponentialSmoothing(realData, bestAlpha);
        double[] forecastMovingAver = movingAverage(realData, bestWindow);

        System.out.println("MISC Exponential Smoothing: " + calculateMISC(subArray(realData, 1), forecastExp) + "%");
        System.out.println("RMSE: " + calculateRSME(realData, forecastExp));
        System.out.println("MAPE: " + calculateMAPE(realData, forecastExp));
        System.out.println("TP: " + calculateTP(realData, forecastExp));
        System.out.println("TN: " + calculateTN(realData, forecastExp));
        System.out.println("FN: " + calculateFN(realData, forecastExp));
        System.out.println("FP: " + calculateFP(realData, forecastExp));

        double[] realDataSubset = subArray(realData, 4, realData.length);
        System.out.println("MISC average: " + calculateMISC(realDataSubset, forecastMovingAver) + "%");
        System.out.println("RMSE: " + calculateRSME(realDataSubset, forecastMovingAver));
        System.out.println("MAPE: " + calculateMAPE(realDataSubset, forecastMovingAver));
        System.out.println("TP: " + calculateTP(realDataSubset, forecastMovingAver));
        System.out.println("TN: " + calculateTN(realDataSubset, forecastMovingAver));
        System.out.println("FN: " + calculateFN(realDataSubset, forecastMovingAver));
        System.out.println("FP: " + calculateFP(realDataSubset, forecastMovingAver));

        Date[] timeExp = subArray(time, 1, time.length);
        timeExp[timeExp.length - 1] = new Date(timeExp[timeExp.length - 2].getTime() + (timeExp[timeExp.length - 2].getTime() - timeExp[timeExp.length - 3].getTime()));

        Date[] timeAver = subArray(time, bestWindow, time.length);
        timeAver[timeAver.length - 1] = new Date(timeAver[timeAver.length - 2].getTime() + (timeAver[timeAver.length - 2].getTime() - timeAver[timeAver.length - 3].getTime()));

        drawPlot(time, realData, timeExp, timeAver, forecastExp, forecastMovingAver);
    }

    public static List<String[]> getData() {
        List<String[]> data = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("CSV_KLINES/KLINES_BATUSDT.csv"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                data.add(line.split(";"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    public static Date parseDate(String dateString) {
        try {
            long timestamp = Long.parseLong(dateString);
            return new Date(timestamp);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static double[] exponentialSmoothing(double[] data, double alpha) {
        double[] forecast = new double[data.length];
        forecast[0] = data[0];

        for (int k = 1; k < data.length; k++) {
            forecast[k] = alpha * data[k - 1] + (1 - alpha) * forecast[k - 1];
        }

        return forecast;
    }

    public static double[] movingAverage(double[] data, int n) {
        double[] movingAver = new double[data.length - n + 1];
        for (int i = 0; i < data.length - n + 1; i++) {
            double sum = 0;
            for (int j = i; j < i + n; j++) {
                sum += data[j];
            }
            movingAver[i] = sum / n;
        }
        return movingAver;
    }

    public static double calculateRSME(double[] realData, double[] forecast) {
        double rsme = 0;
        for (int i = 0; i < realData.length; i++) {
            rsme += Math.pow(realData[i] - forecast[i], 2);
        }
        rsme /= realData.length;
        return Math.sqrt(rsme);
    }

    public static double calculateMAPE(double[] realData, double[] forecast) {
        double sum = 0;
        for (int i = 0; i < realData.length; i++) {
            sum += Math.abs((realData[i] - forecast[i]) / realData[i]);
        }
        return (sum / realData.length) * 100;
    }

    public static int calculateTP(double[] realData, double[] forecast) {
        int tp = 0;
        for (int i = 1; i < realData.length; i++) {
            if ((realData[i] - realData[i - 1]) > 0 && (forecast[i] - forecast[i - 1]) > 0) {
                tp++;
            }
        }
        return tp;
    }

    public static int calculateTN(double[] realData, double[] forecast) {
        int tn = 0;
        for (int i = 1; i < realData.length; i++) {
            if ((realData[i] - realData[i - 1]) < 0 && (forecast[i] - forecast[i - 1]) < 0) {
                tn++;
            }
        }
        return tn;
    }

    public static int calculateFN(double[] realData, double[] forecast) {
        int fn = 0;
        for (int i = 1; i < realData.length; i++) {
            if ((realData[i] - realData[i - 1]) > 0 && (forecast[i] - forecast[i - 1]) < 0) {
                fn++;
            }
        }
        return fn;
    }

    public static int calculateFP(double[] realData, double[] forecast) {
        int fp = 0;
        for (int i = 1; i < realData.length; i++) {
            if ((realData[i] - realData[i - 1]) < 0 && (forecast[i] - forecast[i - 1]) > 0) {
                fp++;
            }
        }
        return fp;
    }

    public static double calculateMISC(double[] realData, double[] forecast) {
        int tp = calculateTP(realData, forecast);
        int tn = calculateTN(realData, forecast);
        int fn = calculateFN(realData, forecast);
        int fp = calculateFP(realData, forecast);
        return ((double) (tp + tn) / (tp + tn + fn + fp)) * 100;
    }

    public static double[] subArray(double[] array, int start, int end) {
        double[] subArray = new double[end - start];
        System.arraycopy(array, start, subArray, 0, end - start);
        return subArray;
    }

    public static double[] subArray(double[] array, int start) {
        return subArray(array, start, array.length);
    }

    public static Date[] subArray(Date[] array, int start, int end) {
        Date[] subArray = new Date[end - start + 1];
        System.arraycopy(array, start, subArray, 0, subArray.length - 1);
        return subArray;
    }

    public static Date[] subArray(Date[] array, int start) {
        return subArray(array, start, array.length);
    }

 

    public static void drawPlot(Date[] time, double[] realData, Date[] timeExp, Date[] timeAver, double[] forecastExp, double[] forecastMovingAver) {
            
            XYChart chart = new XYChartBuilder().width(800).height(600).title("Data and Forecast Plot").build();

            chart.getStyler().setChartBackgroundColor(Color.WHITE);
            chart.getStyler().setPlotBackgroundColor(Color.WHITE);
            chart.getStyler().setPlotGridLinesVisible(true);
            chart.getStyler().setSeriesColors(new Color[] { Color.MAGENTA, Color.GRAY, Color.YELLOW, Color.PINK });
            chart.addSeries("Real Data", getYears(time), realData);  // Add this closing parenthesis
            chart.addSeries("Exponential Smoothing Forecast", getYears(timeExp), forecastExp);
            chart.addSeries("Moving Average Forecast", getYears(timeAver), forecastMovingAver);
            JPanel panel = new XChartPanel<>(chart);
            JFrame frame = new JFrame("Data and Forecast Plot");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            frame.pack();
            frame.setVisible(true);
        }

        private static Year[] getYears(Date[] dates) {
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
            Year[] years = new Year[dates.length];
            for (int i = 0; i < dates.length; i++) {
                years[i] = new Year(yearFormat.format(dates[i]));
            }
            return years;
        }
    }

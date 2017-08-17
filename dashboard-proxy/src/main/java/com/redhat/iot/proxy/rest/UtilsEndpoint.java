/*
 * ******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc. and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *
 * ******************************************************************************
 */
package com.redhat.iot.proxy.rest;

import com.redhat.iot.proxy.model.*;
import com.redhat.iot.proxy.service.DGService;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import java.util.*;

/**
 * A simple REST service which proxies requests to a local datagrid.
 */

@Path("/utils")
@Singleton
public class UtilsEndpoint {

    public static final long MS_IN_HOUR = 60 * 60 * 1000;

    @Inject
    DGService dgService;

    @GET
    @Path("/health")
    public String health() {
        return "ok";
    }

    @POST
    @Path("/resetAll")
    public void resetAll() {

        Map<String, Customer> customerCache = dgService.getCustomers();
        Map<String, Facility> facilitiesCache = dgService.getFacilities();
        Map<String, Line> linesCache = dgService.getProductionLines();
        Map<String, Machine> machinesCache = dgService.getMachines();
        Map<String, Run> runsCache = dgService.getRuns();
        Map<String, CalEntry> calendarCache = dgService.getCalendar();


        facilitiesCache.clear();
        linesCache.clear();
        customerCache.clear();
        machinesCache.clear();
        runsCache.clear();
        calendarCache.clear();

        for (String COMPANY : COMPANIES) {
            customerCache.put(COMPANY,
                    new Customer(COMPANY, "password"));
        }

        for (String[] facility : FACILITIES) {

            Facility newFacility = new Facility();
            newFacility.setName(facility[0]);
            newFacility.setFid(facility[1]);
            newFacility.setSize(Math.random() * 10000);
            newFacility.setLocation(new LatLng(20, -80));
            newFacility.setAddress(newFacility.getName());
            newFacility.setUtilization(Math.random());

            CalEntry calEntry = new CalEntry();
            calEntry.setTitle("Maintenance Window");
            Date now = new Date();
            Date later = new Date(new Date().getTime() + (3 * 60 * 60 * 1000));
            calEntry.setStart(now);
            calEntry.setEnd(later);
            calEntry.setFacility(newFacility);
            calendarCache.put(UUID.randomUUID().toString(), calEntry);
            List<Line> lines = new ArrayList<>();

            for (String[] line : LINES) {
                Line newLine = new Line();
                newLine.setCurrentFid(newFacility.getFid());
                lines.add(newLine);
                newLine.setName(line[0]);
                newLine.setLid(line[1]);
                newLine.setDesc("The line");
                newLine.setStatus("ok");

                List<Machine> machines = new ArrayList<>();

                for (String[] machine : MACHINES) {
                    Machine newMachine = new Machine();
                    machines.add(newMachine);
                    newMachine.setName(machine[0]);
                    newMachine.setMid(machine[1]);
                    newMachine.setStatus("ok");
                    newMachine.setDesc("The machine");
                    List<Telemetry> machineTelemetry = new ArrayList<>();
                    machineTelemetry.add(new Telemetry("A", 40, 15, "Current", "current"));
                    machineTelemetry.add(new Telemetry("°C", 50, 10, "Temperature", "temp"));
                    machineTelemetry.add(new Telemetry("db", 50, 10, "Noise", "noise"));
                    machineTelemetry.add(new Telemetry("rpm", 2000, 1000, "Speed", "speed"));
                    machineTelemetry.add(new Telemetry("nu", 2000, 0, "Vibration", "vibration"));
                    machineTelemetry.add(new Telemetry("V", 250, 190, "Voltage", "voltage"));
                    newMachine.setTelemetry(machineTelemetry);
                    newMachine.setCurrentFid(newFacility.getFid());
                    newMachine.setCurrentLid(newLine.getLid());
                    newMachine.setType(machine[2]);
                    machinesCache.put(newFacility.getFid() + "/" + newLine.getLid() + "/" + newMachine.getMid(), newMachine);


                }

                newLine.setMachines(machines);

                Run r = new Run();
                r.setName(rand(RUNS));
                r.setCustomer(customerCache.get(rand(COMPANIES)));
                r.setDesc("Standard Run");
                newLine.setCurrentRun(r);
                r.setStatus("ok");
                r.setStart(new Date(now.getTime() - ((int)(Math.floor((Math.random()  * 2.0) * (double)MS_IN_HOUR)))));
                r.setEnd(new Date(now.getTime() + ((int)(Math.floor((Math.random()  * 4.0) * (double)MS_IN_HOUR)))));

                CalEntry runEntry = new CalEntry();
                runEntry.setTitle(r.getName() + "(" + r.getCustomer().getName() + ")");
                runEntry.setStart(r.getStart());
                runEntry.setEnd(r.getEnd());
                runEntry.setFacility(newFacility);
                runEntry.setColor(line[2]);
                calendarCache.put(UUID.randomUUID().toString(), runEntry);

                runsCache.put(r.getRid(), r);

                linesCache.put(newFacility.getFid() + "/" + newLine.getLid(), newLine);


            }

            newFacility.setLines(lines);

            facilitiesCache.put(newFacility.getFid(), newFacility);
        }


    }

    private String rand(String[] strs) {
        return strs[(int) Math.floor(Math.random() * strs.length)];
    }

    @GET
    @Path("/summaries")
    @Produces({"application/json"})
    public List<Summary> getSummaries() {

        List<Summary> result = new ArrayList<>();

        Summary customerSummary = getClientSummary();
        Summary runsSummary = getRunsSummary();
        Summary linesSummary = getLinesSummary();
        Summary facilitySummary = getFacilitySummary();
        Summary machinesSummary = getMachinesSummary();

        result.add(customerSummary);
        result.add(runsSummary);
        result.add(linesSummary);
        result.add(facilitySummary);
        result.add(machinesSummary);

        Summary operatorsSummary = new Summary();
        operatorsSummary.setName("operators");
        operatorsSummary.setTitle("Operators");
        operatorsSummary.setCount(23);
        operatorsSummary.setWarningCount(4);
        operatorsSummary.setErrorCount(1);
        result.add(operatorsSummary);
        return result;
    }

    private Summary getFacilitySummary() {
        Map<String, Facility> cache = dgService.getFacilities();

        Summary summary = new Summary();
        summary.setName("facilities");
        summary.setTitle("Facilities");
        summary.setCount(cache.keySet().size());

        long warningCount = cache.keySet().stream()
                .map(cache::get)
                .filter(v -> v.getUtilization() < .7 && v.getUtilization() > .5)
                .count();

        long errorCount = cache.keySet().stream()
                .map(cache::get)
                .filter(v -> v.getUtilization() < .5)
                .count();

        summary.setWarningCount(warningCount);
        summary.setErrorCount(errorCount);

        return summary;
    }

    private Summary getClientSummary() {
        Map<String, Customer> cache = dgService.getCustomers();

        Summary summary = new Summary();
        summary.setName("clients");
        summary.setTitle("Clients");
        summary.setCount(cache.keySet().size());
        return summary;

    }

    private Summary getLinesSummary() {
        Map<String, Run> cache = dgService.getRuns();

        Summary summary = new Summary();
        summary.setName("lines");
        summary.setTitle("Lines");
        summary.setCount(cache.keySet().size());

        long warningCount = cache.keySet().stream()
                .map(cache::get)
                .filter(r -> r.getStatus().equalsIgnoreCase( "warning"))
                .count();

        long errorCount = cache.keySet().stream()
                .map(cache::get)
                .filter(r -> r.getStatus().equalsIgnoreCase("error"))
                .count();

        summary.setWarningCount(warningCount);
        summary.setErrorCount(errorCount);

        return summary;

    }
    private Summary getRunsSummary() {
        Map<String, Run> cache = dgService.getRuns();

        Summary summary = new Summary();
        summary.setName("runs");
        summary.setTitle("Runs");
        summary.setCount(cache.keySet().size());
        long warningCount = cache.keySet().stream()
                .map(cache::get)
                .filter(r -> r.getStatus().equalsIgnoreCase( "warning"))
                .count();

        long errorCount = cache.keySet().stream()
                .map(cache::get)
                .filter(r -> r.getStatus().equalsIgnoreCase("error"))
                .count();

        summary.setWarningCount(warningCount);
        summary.setErrorCount(errorCount);

        return summary;

    }

    private Summary getMachinesSummary() {
        Map<String, Machine> cache = dgService.getMachines();

        Summary summary = new Summary();
        summary.setName("runs");
        summary.setTitle("Runs");
        summary.setCount(cache.keySet().size());
        long warningCount = cache.keySet().stream()
                .map(cache::get)
                .filter(r -> r.getStatus().equalsIgnoreCase( "warning"))
                .count();

        long errorCount = cache.keySet().stream()
                .map(cache::get)
                .filter(r -> r.getStatus().equalsIgnoreCase("error"))
                .count();

        summary.setWarningCount(warningCount);
        summary.setErrorCount(errorCount);

        return summary;

    }

    public static final String[] COMPANIES = new String[]{
            "Wonka Industries",
            "Acme Corp",
            "Stark Industries",
            "Ollivander's Wand Shop",
            "Gekko & Co",
            "Wayne Enterprises",
            "Cyberdyne Systems",
            "Cheers",
            "Genco Pura",
            "NY Enquirer",
            "Duff Beer",
            "Bubba Gump Shrimp Co",
            "Olivia Pope & Associates",
            "Sterling Cooper",
            "Soylent",
            "Hooli",
            "Good Burger"
    };

    public static final String[][] FACILITIES = new String[][]{
            {"Atlanta", "facility-1"},
            {"Singapore", "facility-2"},
            {"Frankfurt", "facility-3"},
            {"Raleigh", "facility-4"}
    };

    public static final String[][] LINES = new String[][]{
            {"Line 1", "line-1", "red"},
            {"Line 2", "line-2", "orange"},
            {"Line 3", "line-3", "yellow"},
            {"Line 4", "line-4", "green"},
            {"Line 5", "line-5", "blue"},
            {"Line 6", "line-6", "indigo"},
            {"Line 7", "line-7", "violet"},
            {"Line 8", "line-8", "cyan"}
    };

    public static final String[][] MACHINES = new String[][]{
            {"Caster", "machine-1", "caster"},
            {"Chiller", "machine-2", "chiller"},
            {"Weighting", "machine-3", "scale"},
            {"Spin Test", "machine-4", "spinner"}
    };

    public static final String[] RUNS = new String[]{
            "500-FS",
            "240-DS",
            "10000-TP"
    };


}

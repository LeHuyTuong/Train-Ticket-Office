package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Route;
import com.example.trainticketoffice.model.Station;
import com.example.trainticketoffice.service.RouteService;
import com.example.trainticketoffice.service.StationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/routes")
public class RouteController {

    private final RouteService routeService;
    private final StationService stationService;

    @Autowired
    public RouteController(RouteService routeService, StationService stationService) {
        this.routeService = routeService;
        this.stationService = stationService;
    }

    @GetMapping
    public String listRoutes(Model model) {
        List<Route> routes = routeService.getAllRoutes();
        model.addAttribute("routes", routes);
        return "route/list";
    }

    private void addCommonAttributes(Model model) {
        List<Station> allStations = stationService.getAllStations();
        model.addAttribute("allStations", allStations);
        model.addAttribute("statusTypes", new String[]{"ACTIVE", "INACTIVE"});
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("route", new Route());
        addCommonAttributes(model);
        return "route/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        Route route = routeService.getAllRoutes().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (route != null) {
            model.addAttribute("route", route);
            addCommonAttributes(model);
            return "route/form";
        }
        return "redirect:/routes";
    }

    @PostMapping("/save")
    public String saveRoute(@Valid @ModelAttribute("route") Route route,
                            BindingResult result, Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            addCommonAttributes(model);
            return "route/form";
        }

        try {
            if (route.getId() == null) {
                Route createdRoute = routeService.createRoute(route);
                if (createdRoute == null) {
                    model.addAttribute("errorMessage", "Route code already exists!");
                    addCommonAttributes(model);
                    return "route/form";
                }
            } else {
                Route updatedRoute = routeService.updateRoute(route.getId(), route);
                if (updatedRoute == null) {
                    model.addAttribute("errorMessage", "Route code already exists or route not found!");
                    addCommonAttributes(model);
                    return "route/form";
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Route saved successfully!");
            return "redirect:/routes";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error saving route: " + e.getMessage());
            addCommonAttributes(model);
            return "route/form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteRoute(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            routeService.deleteRoute(id);
            redirectAttributes.addFlashAttribute("successMessage", "Route with ID " + id + " has been deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting route: " + e.getMessage());
        }
        return "redirect:/routes";
    }

    @GetMapping("/search")
    public String searchRoutesForm(Model model) {
        List<Station> allStations = stationService.getAllStations();
        model.addAttribute("allStations", allStations);
        return "route/search";
    }

    @PostMapping("/search")
    public String searchRoutes(@RequestParam Integer startStationId,
                               @RequestParam Integer endStationId,
                               Model model) {
        try {
            Station startStation = stationService.getStationById(startStationId);
            Station endStation = stationService.getStationById(endStationId);

            if (startStation == null || endStation == null) {
                model.addAttribute("errorMessage", "Start station or end station not found!");
                List<Station> allStations = stationService.getAllStations();
                model.addAttribute("allStations", allStations);
                return "route/search";
            }

            List<Route> routes = routeService.getRoutesByStartAndEndStation(startStation, endStation);
            model.addAttribute("routes", routes);
            model.addAttribute("startStation", startStation);
            model.addAttribute("endStation", endStation);

            List<Station> allStations = stationService.getAllStations();
            model.addAttribute("allStations", allStations);

            return "route/search";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error searching routes: " + e.getMessage());
            List<Station> allStations = stationService.getAllStations();
            model.addAttribute("allStations", allStations);
            return "route/search";
        }
    }
}
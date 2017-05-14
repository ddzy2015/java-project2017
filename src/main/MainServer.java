package main;

import java.util.Date;

import data.Admin;
import data.City;
import data.DataManager;
import data.Flight;
import data.FlightStatus;
import data.Order;
import data.Passenger;
import data.User;
import exceptions.PermissionDeniedException;
import exceptions.StatusUnavailableException;

public class MainServer {
	
	private DataManager dataManager;
	private User currentUser;
	private boolean isLogin;
	private boolean isAdmin;
	
	public MainServer() {
		isLogin = false;
		isAdmin = false;
		dataManager = new DataManager();
	}

	public void stop() {
		dataManager.stop();
	}
	
	public boolean login(String userName, String pass) {
		for (User user : dataManager.users) {
			if (user.getUserName().equals(userName) && user.getPassHash().equals(User.hashPass(pass))) {
				isLogin = true;
				if (user instanceof Admin) {					
					isAdmin = true;
				} else {
					isAdmin = false;
				}
				currentUser = user;
				return true;
			} else {
				isLogin = false;
			}
		}
		return false;
	}
	
	public boolean isLogin() {
		return isLogin;
	}
	
	public boolean isAdmin() {
		return isAdmin;
	}
	
	private void checkPermission(boolean requireAdmin) throws PermissionDeniedException {
		// Require
		if (!isLogin) {
			throw new PermissionDeniedException("please login");
		} else if (requireAdmin && !isAdmin) {
			throw new PermissionDeniedException("you are not administrator");
		}
	}
	
	/*
	 *  TODO(all) main function(search, add, ...) at here, each should decide whether isLogin and isAdmin
	 *  if having no permission, throw PermissionDeniedException
	 *  you can see some example(completed) below to know how to throw it
	 *  **make full use of private method searchFlightByID & searchUserByID**
	 */
	public boolean createFlight(String flightName, Date startTime, Date arriveTime, int startCityID, int arriveCityID,
			int price, int seatCapacity) throws PermissionDeniedException { // false when error cityID
		// DONE(Peng) creatFlight
		checkPermission(true);
		try {
			dataManager.flights.add(new Flight(flightName,startTime, arriveTime,dataManager.getCityByID(startCityID),dataManager.getCityByID(arriveCityID),
					price,seatCapacity));
			return true;
		} catch (NullPointerException e) {
			return false;
		}
	}
	
	public Flight getFlight(int flightID) throws PermissionDeniedException { //give you flight to change freely
		checkPermission(true);
		return dataManager.getFlightByID(flightID);
	}
	
	public City getCity(int cityID) throws PermissionDeniedException { //give you city to change freely
		checkPermission(true);
		return dataManager.getCityByID(cityID);	
	}
	
	public boolean deleteFlight(int flightID) throws PermissionDeniedException, StatusUnavailableException	 {
		/* DONE(Peng) deleteFlight
		 * **be sure to delete flight from the city**
		 * FIXME (Peng) be careful about the comment above!
		 */
		checkPermission(true);
		Flight f =dataManager.getFlightByID(flightID);
		if (f != null) {
			if (f.getFlightStatus() == FlightStatus.UNPUBLISHED) {
				dataManager.flights.remove(f);
				return true;
			} else {
				throw new StatusUnavailableException(f.getFlightStatus());
			}
		}
		return false; 
	}
	
	public void queryFlight() {
		// XXX I think this can be changed to a class
	}
	
	public void superQuery() { 
		// XXX I think this can be changed to a class extends queryFlight
	}
	
	public void addPassenger(String username, String idNumber, String password){
		dataManager.users.add(new Passenger(idNumber, username, password));
	}
	
	public void addAdmin(String userName, String password) throws PermissionDeniedException {
		// DONE(Peng) addAdmin
		checkPermission(true);
		dataManager.users.add(new Admin(userName, password));
	}
	
	public void addCity(String cityName) throws PermissionDeniedException{
		//DONE(Zhu) addCity
		checkPermission(true);
		dataManager.cities.add(new City(cityName));
	}
	
	public boolean deleteUser(int userID) throws PermissionDeniedException {
		/* TODO(Peng) deleteUser
		 * first to test if user is a passenger (using instanceof)
		 * **be sure to remove user from the flight**
		 */
		checkPermission(true);
		User u = dataManager.getUserByID(userID);
		if (u == null) {
			return false;
		}
		if (u instanceof Passenger) {
			Passenger passenger = (Passenger) u;
			for (Order order : passenger.orderList()) {
				order.remove();
			}
			dataManager.users.remove(u);
		} else {
			dataManager.users.remove(u);
		}
		return true;
	}
	
	public User getCurrentUser() throws PermissionDeniedException { // to update user info
		checkPermission(true);
		return currentUser;
	}
	
	public boolean reserveFlight(int flightID) throws PermissionDeniedException, StatusUnavailableException {
		// DONE(Zhu) reserveFlight
		checkPermission(false);
		if (isAdmin) {
			throw new PermissionDeniedException("adminstrator cannot reserve flight");
		}
		Flight flight = dataManager.getFlightByID(flightID);
		if (flight != null) {
			((Passenger) currentUser).reserveFlight(flight);
			return true;
		}
		return false;
	}
	
	public boolean unsubscribeFlight(int flightID) throws PermissionDeniedException, StatusUnavailableException { //return false when no flight is found
		//DONE(Peng) unsubscribeFlight
		checkPermission(false);
		if (!isAdmin) {
			Passenger passenger = (Passenger) getCurrentUser();
			Flight flight = dataManager.getFlightByID(flightID);
			if (flight != null) {
				passenger.unsubscribeFlight(flight);
				return true;
			}
		} else {
			throw new StatusUnavailableException("Only passengers can unsubscribe flight");
		}
		return false;
	}
	
	public void pay(int index) throws PermissionDeniedException, StatusUnavailableException {
		// TODO(Peng) pay an order (index is the index of the order in ArrayList<Order>)
		
	}
	
	public void cancel(int index) throws PermissionDeniedException, StatusUnavailableException{
		// TODO(Peng) cancel an order (index is the index of the order in ArrayList<Order>)
	}
	
	public boolean deleteCity(int cityID) throws PermissionDeniedException, StatusUnavailableException {
		checkPermission(true);
		City city = dataManager.getCityByID(cityID);
		if (city != null) {
			if (city.getFlightsIn().size() == 0 && city.getFlightsOut().size() == 0) {
				dataManager.cities.remove(city);					
			} else {
				throw new StatusUnavailableException("Cannot delete city that have fight in and out");
			}
			return true;
		}
		return false;
	}
	
	/*
	 * return a string with this format:
	 * ID	Name	....
	 * 1	A001	....
	 * .	.		.
	 * .	.		 .
	 * tips: see <Flight>.toString() and <Order>.toString()
	 */
	public String displayCity() {
		// TODO(Zhu)
		return null;
	}

	public String displayFlight() {
		// TODO(Zhu) 
		return null;
	}

	public String dispalyUser() {
		// TODO(Peng)
		return null;
	}
	
	public String displayFlight(int flightID) {
		// DONE(Dong)
		StringBuilder resultbuilder = new StringBuilder();
		Flight flight = dataManager.getFlightByID(flightID);
		resultbuilder.append(flight.toString() + "\n");
		if (isAdmin) {
			resultbuilder.append("\tPasengers:\n");
			for (Passenger passenger : flight.passagers().keySet()) {
				resultbuilder.append("\t\t" + passenger.toString() + "\n");
			}
		}
		return resultbuilder.toString();
	}
	/*
	 * when it comes to display a object with specific id, you need provide more specific information(see above)
	 */
	public String displayCity(int CityID) {
		// TODO(Peng) print flightIn and flightOut as well
		return null;
	}
	
	public String dispalyUser(int UserID) {
		// TODO(Zhu) print User order(if it is passenger) as well
		return null;
	}
	
	public String displayOrder() throws PermissionDeniedException {
		// TODO(Zhu) print all the order in order (if isLogin)
		return null;
		
	}
	//------------boundary-----------------

	public String search(int cityFromId, int cityToId, Date date1, Date date2) {
		// TODO(Dong) The most difficult one
		return null;
	}

}

package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.ResourceNotFoundException;
import com.example.trainticketoffice.model.*;
import com.example.trainticketoffice.repository.TripRepository;
import com.example.trainticketoffice.service.BookingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/**
 * Controller chịu trách nhiệm xử lý toàn bộ luồng đặt vé (booking) của người dùng,
 * bao gồm chọn ghế, nhập thông tin hành khách, xử lý đặt vé (một chiều và khứ hồi),
 * xác nhận và quản lý các booking đã có.
 */
@Controller
@RequestMapping("/bookings") // Tất cả các URL trong controller này đều bắt đầu bằng /bookings
@RequiredArgsConstructor // Tự động tiêm (inject) các dependency (Service, Repository) qua constructor
public class BookingController {

    // Service chứa logic nghiệp vụ chính
    private final BookingService bookingService;
    // Repository để truy vấn trực tiếp thông tin Chuyến đi (Trip) khi cần thiết cho view
    private final TripRepository tripRepository;


    /**
     * [STEP 1]
     * Hiển thị trang chọn ghế cho một chuyến đi (Trip) cụ thể.
     *
     * @param tripId  ID của chuyến đi (lấy từ URL query parameter)
     * @param model   Đối tượng để truyền dữ liệu (trip, sơ đồ ghế,...) ra view
     * @param context (Tùy chọn) Dùng để xử lý luồng khứ hồi, xác định đây là "outbound" (lượt đi) hay "inbound" (lượt về)
     * @param session Để kiểm tra thông tin đăng nhập của người dùng
     * @return Tên của view template (ticket/form) hoặc chuyển hướng (redirect) nếu có lỗi
     */
    @GetMapping("/new")
    public String showCreateForm(@RequestParam("tripId") Long tripId, Model model,
                                 @RequestParam(value = "context", required = false) String context,
                                 HttpSession session) {

        // 1. Kiểm tra đăng nhập
        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập, chuyển về trang login
        }

        try {
            // 2. Gọi Service để lấy toàn bộ dữ liệu cần thiết cho trang chọn ghế
            // (Bao gồm thông tin chuyến đi, sơ đồ tàu, danh sách ghế đã đặt,...)
            Map<String, Object> modelData = bookingService.getBookingFormDetails(tripId, currentUser);

            // 3. Thêm tất cả dữ liệu từ Service vào Model
            model.addAllAttributes(modelData);
            // 4. Truyền 'context' (outbound/inbound) ra view để view biết cách xử lý
            model.addAttribute("context", context);

            // 5. Trả về trang "ticket/form.html"
            return "ticket/form";
        } catch (ResourceNotFoundException e) {
            // 6. Xử lý lỗi nếu không tìm thấy Chuyến đi
            // (Service sẽ ném lỗi này)
            return "redirect:/"; // Về trang chủ
        } catch (IllegalStateException e) {
            // 7. Xử lý lỗi nghiệp vụ (ví dụ: Ga tàu thiếu thông tin KM)
            // (Service sẽ ném lỗi này)
            model.addAttribute("errorMessage", e.getMessage());
            return "customer/Home"; // Về trang chủ với thông báo lỗi
        }
    }

    /**
     * [STEP 2]
     * Hiển thị form nhập thông tin hành khách sau khi người dùng đã chọn ghế.
     *
     * @param tripId             ID của chuyến đi
     * @param context            (Tùy chọn) Bối cảnh khứ hồi ("outbound" / "inbound")
     * @param seatIds            Danh sách các ID của ghế mà người dùng đã chọn (từ form STEP 1)
     * @param model              Đối tượng để truyền dữ liệu (BookingRequest DTO, Trip) ra view
     * @param session            Để kiểm tra đăng nhập
     * @param redirectAttributes Dùng để gửi thông báo lỗi nếu chuyển hướng (redirect)
     * @return Tên của view template (ticket/passenger-form) hoặc chuyển hướng
     */
    @GetMapping("/passenger-details")
    public String showPassengerDetailsForm(@RequestParam("tripId") Long tripId,
                                           @RequestParam(value = "context", required = false) String context, // <-- Nhận context
                                           @RequestParam(value = "seatIds", required = false) List<Long> seatIds,
                                           Model model, HttpSession session,
                                           RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra đăng nhập
        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 2. Kiểm tra bắt buộc: Phải chọn ít nhất 1 ghế
        if (seatIds == null || seatIds.isEmpty()) {
            // Nếu không chọn ghế nào, quay lại trang chọn ghế (STEP 1) với thông báo lỗi
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một ghế.");
            return "redirect:/bookings/new?tripId=" + tripId;
        }

        try {
            // 3. Gọi Service để chuẩn bị một đối tượng BookingRequest DTO
            // DTO này sẽ chứa danh sách hành khách (với số lượng bằng số ghế đã chọn)
            // và được dùng để binding (liên kết) với form nhập thông tin
            BookingRequest bookingRequest = bookingService.preparePassengerDetails(tripId, seatIds, currentUser);

            // 4. Gán 'context' vào DTO để nó được gửi đi cùng form ở STEP 3
            bookingRequest.setContext(context);

            // 5. Lấy thông tin Chuyến đi để hiển thị tóm tắt trên view
            Trip trip = tripRepository.findById(tripId).get();

            // 6. Đưa DTO và Trip vào Model
            model.addAttribute("bookingRequest", bookingRequest); // Dùng cho th:object
            model.addAttribute("trip", trip); // Hiển thị thông tin

            // 7. Trả về trang "ticket/passenger-form.html"
            return "ticket/passenger-form";

        } catch (ResourceNotFoundException e) {
            // 8. Xử lý lỗi nếu không tìm thấy Chuyến đi hoặc Ghế
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings/new?tripId=" + tripId; // Quay lại STEP 1
        }
    }

    /**
     * [STEP 3]
     * Xử lý việc tạo Đơn hàng (Order) sau khi người dùng gửi form thông tin hành khách.
     * Đây là phương thức cốt lõi, xử lý logic khứ hồi phức tạp.
     *
     * @param bookingRequest     Đối tượng DTO chứa toàn bộ thông tin từ form (tripId, passengers, context)
     * @param bindingResult      Đối tượng chứa kết quả validation (kiểm tra lỗi @Valid)
     * @param model              Dùng để truyền dữ liệu lại cho view nếu validation thất bại
     * @param session            Dùng để quản lý luồng khứ hồi (lưu trữ thông tin lượt về, group ID)
     * @param redirectAttributes Dùng để truyền thông báo (lỗi, thành công) hoặc ID đơn hàng sang trang khác
     * @return Chuyển hướng (redirect) đến trang xác nhận (nếu thành công) hoặc trang tìm kiếm lượt về (nếu là khứ hồi)
     */
    @PostMapping("/create-order")
    public String createOrder(@Valid @ModelAttribute("bookingRequest") BookingRequest bookingRequest,
                              BindingResult bindingResult,
                              Model model,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra đăng nhập
        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 2. Kiểm tra validation (ví dụ: tên, sđt hành khách không được trống)
        if (bindingResult.hasErrors()) {
            // Nếu có lỗi, phải tải lại thông tin Trip (vì view 'passenger-form' cần)
            Trip trip = tripRepository.findById(bookingRequest.getTripId()).orElse(null);
            model.addAttribute("trip", trip);
            // Trả về lại trang form (STEP 2) để hiển thị lỗi, dữ liệu người dùng đã nhập vẫn được giữ
            return "ticket/passenger-form";
        }

        try {
            // 3. Xử lý logic khứ hồi (Core logic của Controller)
            // Lấy thông tin về lượt về (nếu có) và group ID (nếu có) từ session
            RoundTripInfo nextLeg = (RoundTripInfo) session.getAttribute("roundTripNextLeg");
            String context = bookingRequest.getContext();
            String sessionGroupId = (String) session.getAttribute("roundTripGroupId");

            // Biến này sẽ được lưu vào DB để nhóm 2 booking (đi và về) lại với nhau
            String groupIdToSave = null;

            if ("outbound".equals(context) && nextLeg != null) {
                // 3.1. Đây là LƯỢT ĐI của một chuyến khứ hồi
                // Tạo một Group ID mới (dùng UUID để đảm bảo duy nhất)
                sessionGroupId = UUID.randomUUID().toString();
                // Lưu Group ID này vào session để dùng cho LƯỢT VỀ
                session.setAttribute("roundTripGroupId", sessionGroupId);
                // Gán ID này để lưu vào DB cho booking lượt đi
                groupIdToSave = sessionGroupId;

            } else if ("inbound".equals(context) && sessionGroupId != null) {
                // 3.2. Đây là LƯỢT VỀ của một chuyến khứ hồi
                // Lấy Group ID đã tạo ở lượt đi (từ session)
                // Gán ID này để lưu vào DB cho booking lượt về
                groupIdToSave = sessionGroupId;
            }
            // 3.3. Nếu là vé 1 chiều (context != "outbound" hoặc nextLeg == null), groupIdToSave sẽ là null


            // 4. Gọi Service để tạo Đơn hàng (Order) và các vé (Bookings)
            // Service chỉ làm nhiệm vụ lưu DB, không quan tâm đến logic session/khứ hồi
            Order createdOrder = bookingService.createOrder(bookingRequest, currentUser, groupIdToSave);


            // 5. Xử lý điều hướng (Logic của Controller) sau khi tạo Order thành công
            if ("outbound".equals(context) && nextLeg != null) {
                // 5.1. Nếu vừa đặt xong LƯỢT ĐI (khứ hồi):
                // Chuyển hướng người dùng đến trang tìm kiếm LƯỢT VỀ
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã đặt xong lượt đi (Mã ĐH: " + createdOrder.getOrderId() + "). Vui lòng chọn lượt về.");

                // Tạo URL tìm kiếm cho lượt về dựa trên thông tin đã lưu trong session (nextLeg)
                String returnUrl = String.format("/trips/search?startStationId=%d&endStationId=%d&departureDate=%s&roundTripFlow=return",
                        nextLeg.getStartStationId(),
                        nextLeg.getEndStationId(),
                        nextLeg.getDepartureDate().toString());

                return "redirect:" + returnUrl; // Chuyển hướng
            }

            // 5.2. Nếu là vé 1 chiều HOẶC vừa đặt xong LƯỢT VỀ (khứ hồi):
            // Chuyển đến trang xác nhận / thanh toán
            redirectAttributes.addFlashAttribute("newOrderId", createdOrder.getOrderId());

            // Nếu là LƯỢT VỀ, nghĩa là đã hoàn tất luồng khứ hồi
            // Xóa các thông tin session tạm thời
            if ("inbound".equals(context)) {
                session.removeAttribute("roundTripNextLeg");
                session.removeAttribute("roundTripGroupId");
            }

            // Chuyển hướng đến trang xác nhận
            return "redirect:/bookings/confirm";

        } catch (IllegalArgumentException | IllegalStateException | ResourceNotFoundException ex) {
            // 6. Xử lý lỗi nghiệp vụ (ví dụ: ghế đã bị người khác đặt, hết thời gian giữ chỗ)
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            // Trả về trang chọn ghế (STEP 1) của chuyến đi này
            return "redirect:/bookings/new?tripId=" + bookingRequest.getTripId();
        }
    }

    /**
     * [STEP 4]
     * Hiển thị trang xác nhận và thanh toán.
     * Trang này sẽ hiển thị thông tin của Đơn hàng (Order) vừa được tạo.
     * Nếu là khứ hồi, nó sẽ hiển thị cả 2 booking (đi và về).
     *
     * @param model              Để truyền dữ liệu (Order, Bookings, tổng tiền) ra view
     * @param session            Để kiểm tra đăng nhập và lấy ID đơn hàng (phòng trường hợp F5)
     * @param redirectAttributes Để báo lỗi nếu không tìm thấy đơn hàng
     * @param newOrderId         ID của đơn hàng vừa tạo (được truyền từ `createOrder` qua RedirectAttributes)
     * @return Tên view template (payment/confirm-payment)
     */
    @GetMapping("/confirm")
    public String showConfirmPage(Model model, HttpSession session,
                                  RedirectAttributes redirectAttributes,
                                  @ModelAttribute("newOrderId") Long newOrderId) {

        // 1. Kiểm tra đăng nhập
        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 2. Logic để lấy Order ID (Xử lý trường hợp F5)
        Long orderIdToLoad = newOrderId;

        if (orderIdToLoad == null || orderIdToLoad == 0) {
            // 2.1. Nếu không có ID từ redirect (ví dụ: user F5 trang),
            // thử lấy ID từ session (đã lưu ở lần tải trước)
            orderIdToLoad = (Long) session.getAttribute("lastNewOrderId");
            if (orderIdToLoad == null) {
                // Nếu không có ID nào, không biết hiển thị đơn hàng nào -> về trang quản lý vé
                return "redirect:/bookings";
            }
        } else {
            // 2.2. Nếu có ID mới từ redirect, lưu nó vào session để phòng F5
            session.setAttribute("lastNewOrderId", orderIdToLoad);
        }

        try {
            // 3. Gọi Service để lấy toàn bộ thông tin chi tiết cho trang xác nhận
            // Service sẽ tự động kiểm tra xem Order này có "groupId" không,
            // nếu có, nó sẽ tải cả booking lượt đi và về.
            Map<String, Object> modelData = bookingService.getConfirmationDetails(orderIdToLoad, currentUser);

            // 4. Thêm dữ liệu (Orders, tổng tiền,...) vào Model
            model.addAllAttributes(modelData);

            // 5. Trả về trang "payment/confirm-payment.html"
            return "payment/confirm-payment";

        } catch (ResourceNotFoundException e) {
            // 6. Xử lý lỗi nếu không tìm thấy Đơn hàng
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings"; // Về trang quản lý vé
        }
    }

    /**
     * [QUẢN LÝ BOOKING]
     * Hiển thị danh sách tất cả các booking (vé) của người dùng hiện tại.
     *
     * @param session Để lấy thông tin người dùng đang đăng nhập
     * @param model   Để truyền danh sách booking ra view
     * @return Tên view template (ticket/list)
     */
    @GetMapping
    public String listBookings(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Lấy tất cả booking của user
        List<Booking> bookings = bookingService.findAllBookingsByUserId(currentUser.getId());

        model.addAttribute("bookings", bookings);
        return "ticket/list"; // Trả về trang "ticket/list.html"
    }

    /**
     * [QUẢN LÝ BOOKING]
     * Hiển thị chi tiết một booking (vé) cụ thể.
     *
     * @param bookingId ID của booking (lấy từ path variable)
     * @param model     Để truyền thông tin booking ra view
     * @param redirectAttributes Để báo lỗi nếu không tìm thấy
     * @return Tên view template (ticket/detail)
     */
    @GetMapping("/{bookingId}")
    public String viewBooking(@PathVariable Long bookingId,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        // Tìm booking theo ID
        Optional<Booking> booking = bookingService.findById(bookingId);

        if (booking.isEmpty()) {
            // Nếu không tìm thấy, quay về trang danh sách
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin đặt vé");
            return "redirect:/bookings";
        }

        model.addAttribute("booking", booking.get());
        return "ticket/detail"; // Trả về trang "ticket/detail.html"
    }

    /**
     * [QUẢN LÝ BOOKING]
     * Xử lý yêu cầu hủy vé từ phía người dùng.
     * (Thường dùng GET cho link hủy đơn giản, dù POST/DELETE đúng chuẩn REST hơn)
     *
     * @param bookingId ID của booking cần hủy
     * @param session   Để lấy thông tin người dùng (đảm bảo đúng chủ vé)
     * @param redirectAttributes Để gửi thông báo (thành công/lỗi)
     * @return Chuyển hướng về trang danh sách booking
     */
    @GetMapping("/delete/{bookingId}")
    public String deleteBooking(@PathVariable Long bookingId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra đăng nhập
        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            // 2. Gọi Service để thực hiện hủy vé
            // Service sẽ kiểm tra các điều kiện (ví dụ: chỉ được hủy trước 24h)
            // và xác thực xem user này có quyền hủy vé này không
            bookingService.customerCancelBooking(bookingId, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy booking " + bookingId + " thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // 3. Xử lý nếu hủy thất bại (do lỗi nghiệp vụ)
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }

        // 4. Luôn chuyển hướng về trang danh sách booking
        return "redirect:/bookings";
    }
}

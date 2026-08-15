import { create } from "zustand";
import { SeatStatus } from "@/lib/types";

type BookingState = {
  userId: string;
  trainId: string;
  userTier: "NORMAL" | "PREMIUM";
  selectedSeatId?: number;
  selectedSeatNumber?: string;
  queueToken?: string;
  holdExpiresAt?: string;
  seats: Array<{ id: number; seatNumber: string; status: SeatStatus }>;
  setUser: (userId: string, trainId: string, userTier: "NORMAL" | "PREMIUM") => void;
  setSelectedSeat: (seatId: number, seatNumber: string) => void;
  setQueueToken: (token?: string) => void;
  setHoldExpiresAt: (at?: string) => void;
  setSeats: (seats: BookingState["seats"]) => void;
  updateSeatStatus: (seatId: number, status: SeatStatus) => void;
};

const initialSeats = Array.from({ length: 50 }, (_, i) => ({
  id: i + 1,
  seatNumber: `S${String(i + 1).padStart(2, "0")}`,
  status: "AVAILABLE" as SeatStatus
}));

export const useBookingStore = create<BookingState>((set) => ({
  userId: "u-123",
  trainId: "12951",
  userTier: "NORMAL",
  seats: initialSeats,
  setUser: (userId, trainId, userTier) => set({ userId, trainId, userTier }),
  setSelectedSeat: (selectedSeatId, selectedSeatNumber) => set({ selectedSeatId, selectedSeatNumber }),
  setQueueToken: (queueToken) => set({ queueToken }),
  setHoldExpiresAt: (holdExpiresAt) => set({ holdExpiresAt }),
  setSeats: (seats) => set({ seats }),
  updateSeatStatus: (seatId, status) =>
    set((state) => ({ seats: state.seats.map((s) => (s.id === seatId ? { ...s, status } : s)) }))
}));

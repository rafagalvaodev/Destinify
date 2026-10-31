const KEY = 'destinify.bookingDraft';

export function readBookingDraft(storage = localStorage) {
  try {
    const draft = JSON.parse(storage.getItem(KEY));
    if (draft?.version !== 1 || !Number.isInteger(draft.hotel?.hotel_id)
        || !Array.isArray(draft.rooms) || !draft.rooms.length
        || draft.rooms.some((room) => !Number.isInteger(room.room_id) || room.hotel_id !== draft.hotel.hotel_id)
        || typeof draft.arrival !== 'string' || typeof draft.departure !== 'string'
        || !['string', 'number'].includes(typeof draft.guests)
        || (draft.adultsCount != null && !['string', 'number'].includes(typeof draft.adultsCount))
        || (draft.childrenAges != null && (!Array.isArray(draft.childrenAges)
          || draft.childrenAges.length > 59
          || draft.childrenAges.some((age) => !['string', 'number'].includes(typeof age))))) return null;
    return draft;
  } catch {
    return null;
  }
}

export function saveBookingDraft(draft, storage = localStorage) {
  storage.setItem(KEY, JSON.stringify({ ...draft, version: 1 }));
}

export function clearBookingDraft(storage = localStorage) {
  storage.removeItem(KEY);
}

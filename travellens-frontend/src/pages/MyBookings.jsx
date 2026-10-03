import { useEffect, useState } from 'react'
import api from '../api/axios.js'

export default function MyBookings() {
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = () => {
    setLoading(true)
    api
      .get('/bookings/me')
      .then((res) => setBookings(res.data))
      .catch(() => setError('Could not load your bookings.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  const cancel = async (id) => {
    try {
      await api.delete(`/bookings/${id}`)
      load()
    } catch {
      setError('Could not cancel that booking.')
    }
  }

  return (
    <div className="page">
      <h1>My bookings</h1>
      <p className="muted">Consultations you've reserved with destination advisors.</p>

      {loading && <p className="muted">Loading&hellip;</p>}
      {error && <p className="form-error">{error}</p>}

      {!loading && bookings.length === 0 && (
        <p className="muted">You haven't booked a consultation yet. Find an advisor on a destination page.</p>
      )}

      <div className="booking-list">
        {bookings.map((b) => (
          <div className="booking-row" key={b.id}>
            <div>
              <h4>{b.advisor?.name}</h4>
              <p className="muted small">
                {b.availability?.date} &middot; {b.availability?.startTime} &ndash; {b.availability?.endTime}
              </p>
              <span className={`status-tag status-${b.status?.toLowerCase()}`}>{b.status}</span>
            </div>
            {b.status === 'CONFIRMED' && (
              <button className="btn btn-outline btn-small" onClick={() => cancel(b.id)}>
                Cancel
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

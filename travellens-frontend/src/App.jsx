import { Routes, Route, Navigate } from 'react-router-dom'
import Navbar from './components/Navbar.jsx'
import Destinations from './pages/Destinations.jsx'
import DestinationDetail from './pages/DestinationDetail.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import MyBookings from './pages/MyBookings.jsx'
import CreatePost from './pages/CreatePost.jsx'
import { useAuth } from './context/AuthContext.jsx'

function RequireAuth({ children }) {
  const { user, loading } = useAuth()
  if (loading) return null
  if (!user) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <div className="app-shell">
      <Navbar />
      <main className="app-main">
        <Routes>
          <Route path="/" element={<Destinations />} />
          <Route path="/destinations/:id" element={<DestinationDetail />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/my-bookings"
            element={
              <RequireAuth>
                <MyBookings />
              </RequireAuth>
            }
          />
          <Route
            path="/share"
            element={
              <RequireAuth>
                <CreatePost />
              </RequireAuth>
            }
          />
        </Routes>
      </main>
      <footer className="app-footer">
        <span>TravelLens &mdash; a class project. Discover, consult, plan, share.</span>
      </footer>
    </div>
  )
}

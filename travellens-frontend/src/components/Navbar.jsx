import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <header className="navbar">
      <Link to="/" className="navbar-brand">
        Travel<em>Lens</em>
      </Link>
      <nav className="navbar-links">
        <Link to="/">Destinations</Link>
        {user && <Link to="/share">Share a trip</Link>}
        {user && <Link to="/my-bookings">My bookings</Link>}
        {user ? (
          <div className="navbar-user">
            <span>{user.name}</span>
            <button className="link-button" onClick={handleLogout}>
              Log out
            </button>
          </div>
        ) : (
          <div className="navbar-user">
            <Link to="/login">Log in</Link>
            <Link to="/register" className="navbar-cta">
              Join
            </Link>
          </div>
        )}
      </nav>
    </header>
  )
}

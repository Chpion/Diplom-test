export default {
  setToken(state, token) {
    state.token = token
    if (token) {
      localStorage.setItem('token', token)
    } else {
      localStorage.removeItem('token')
    }
  },
  setUser(state, user) {
    state.user = user
  },
  logout(state) {
    state.token = null
    state.user = null
    localStorage.removeItem('token')
  },
}

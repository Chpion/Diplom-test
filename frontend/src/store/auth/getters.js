export default {
  isAuthenticated: (state) => !!state.token,
  user: (state) => state.user,
  isAdmin: (state) => state.user?.roles?.includes('ROLE_ADMIN') || false,
}

import state from './state'
import mutations from './mutations'
import actions from './action.js'
import getters from './getters'

const authModule = {
  namespaced: true,
  state,
  mutations,
  actions,
  getters,
}

console.log('Auth module initialized:', authModule)
export default authModule

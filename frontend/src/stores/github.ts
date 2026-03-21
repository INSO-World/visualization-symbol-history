import { defineStore } from 'pinia'
import githubUsername from 'github-username'

export type GitHubStore = ReturnType<typeof useGitHubStore>

const MOCK_GITHUB_USERNAME_PRIMARY: string = 'octocat'

const STORAGE_KEY = 'ghusernames'

export const useGitHubStore = defineStore('github', {
  state: () => ({
    usernames: new Map<string, string | null>(),
  }),
  actions: {
    init() {
      const rawArray = localStorage.getItem(STORAGE_KEY)
      if (rawArray == null) {
        localStorage.setItem(STORAGE_KEY, '[]')
      } else {
        const array: Array<[string, string | null]> = JSON.parse(rawArray)
        this.usernames = new Map<string, string | null>(array)
      }
    },
    async getUsername(email: string): Promise<string> {
      if (this.usernames.has(email)) {
        return this.usernames.get(email)!
      }
      return githubUsername(email)
        .then((username) => {
          if (username == null) {
            throw new Error('No username found')
          }
          this._setMapping(email, username)
          return username
        })
        .catch(() => {
          this._setMapping(email, null)
          // FUTURE: Add error handling for failing username retrieval
          return MOCK_GITHUB_USERNAME_PRIMARY
        })
    },
    _setMapping(email: string, username: string | null): void {
      const entries = [...this.usernames.entries()]
      localStorage.setItem(STORAGE_KEY, JSON.stringify(entries))
      this.usernames.set(email, username)
    },
  },
})

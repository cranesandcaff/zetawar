(ns zetawar.events.game
  (:require
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.app :as app]
   [zetawar.game :as game]
   [zetawar.router :as router]))

(defmethod router/handle-event ::execute-action
  [{:as handler-ctx :keys [db]} [_ action]]
  (let [game (app/current-game db)]
    (case (:action/type action)
      :action.type/attack-unit
      (let [{:keys [action/attacker-q action/attacker-r
                    action/defender-q action/defender-r]} action
            [attacker-damage defender-damage] (game/battle-damage db game
                                                                  attacker-q attacker-r
                                                                  defender-q defender-r)
            action (merge action {:action/attacker-damage attacker-damage
                                  :action/defender-damage defender-damage})]
        {:tx     (game/action-tx db game action)
         :notify [[:zetawar.players/apply-action :faction.color/all action]]})

      :action.type/end-turn
      (let [game (app/current-game db)
            next-faction-color (game/next-faction-color game)]
        {:tx       (game/action-tx db game action)
         :dispatch [[:zetawar.events.ui/set-url-game-state]]
         :notify   [[:zetawar.players/apply-action :faction.color/all action]
                    [:zetawar.players/start-turn next-faction-color]]})

      {:tx     (game/action-tx db game action)
       :notify [[:zetawar.players/apply-action :faction.color/all action]]})))

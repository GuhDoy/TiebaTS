(function (send) {
  XMLHttpRequest.prototype.send = function () {
    var callback = this.onreadystatechange;
    this.onreadystatechange = function () {
      if (
        this.readyState == 4 &&
        this.responseURL.match(
          /https?:\/\/tieba\.baidu\.com\/c\/f\/frs\/frsBottom.*/g
        )
      ) {
        res = JSON.parse(this.response);
        let propertiesToDelete = [
          "frs_bottom",
          "activityhead",
          "live_fuse_forum",
          "card_activity",
          "ai_chatroom_guide",
          "friend_forum",
          "game_card_guide"
        ];
        propertiesToDelete.forEach((property) => {
          delete res[property];
        });
        Object.defineProperty(this, "response", { writable: true });
        Object.defineProperty(this, "responseText", {
          writable: true,
        });
        this.response = this.responseText = JSON.stringify(res);
      }
      if (callback) {
        callback.apply(this, arguments);
      }
    };
    send.apply(this, arguments);
  };
})(XMLHttpRequest.prototype.send);

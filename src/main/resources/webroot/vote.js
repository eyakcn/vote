$(function() {
  $('#candidates').on('click.li', 'li', function(e) {
    $('#candidates').find('li.selected').removeClass('selected');
    $(this).addClass('selected');
  });
  /**
   * 检测微信JsAPI
   * @param callback
   */
  function detectWeixinApi(callback){
      if(typeof window.WeixinJSBridge == 'undefined' || typeof window.WeixinJSBridge.invoke == 'undefined'){
          setTimeout(function(){
              detectWeixinApi(callback);
          },200);
      }else{
          callback();
      }
  }

  detectWeixinApi(function(){
      var html = [];
      for(var key in window.WeixinJSBridge) {
          var js = 'WeixinJSBridge.' + key + ' = ' + window.WeixinJSBridge[key].toString();
          js = js_beautify(js); // 美化一下，看着舒服些
          html.push('<pre class="brush:js;toolbar:false;">' + js + '</pre>')
      }

      document.getElementById('WeixinJsApi').innerHTML = html.join('');

      // 代码高亮
      SyntaxHighlighter.highlight();
  });
});
define([
    'app',
	'dataholder',
  'text!./image.html!strip',
  'rivets',
  'views/searches/image',
	], function( app, dataholder, template, rivets, imageSearch ) {
	return Backbone.View.extend({
            title: 'Image',
            count: 0,
            initialize : function() {
          var self = this;
          var scope = {
              view: this,
              isOdd: function() {
                  return (this.view.count++ % 2) ? 'even' : 'odd';
              },

              setClass: function(image) {
                  var image = this.image;
                  return inferImage(image.attributes.location, image.attributes.description, image.attributes.platform);
              },

              search: new imageSearch(app.data.images),
              
              select: function(e, images) {
                $(e.currentTarget).parent().find('tr').removeClass('selected-row');
                $(e.currentTarget).addClass('selected-row');
                self.model.set('image_id', images.image.id);
                self.model.set('image_platform', images.image.attributes.platform ? images.image.attributes.platform : 'Linux');
                self.model.set('image_location', images.image.attributes.location);
                self.model.set('image_description', images.image.attributes.description);
                self.model.set('image_iconclass', this.setClass(images.image));
              }
          } 
          scope.images = scope.search.filtered;
          this.scope = scope;

         $(this.el).html(template)
         this.rView = rivets.bind(this.$el, this.scope);
         this.render();
        },

        render: function() {
          this.rView.sync();
        }
    });
});
